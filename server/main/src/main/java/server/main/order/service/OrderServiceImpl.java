package server.main.order.service;

import static server.main.global.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.main.blockchain.service.BlockchainOutboxService;
import server.main.global.error.BusinessException;
import server.main.global.security.CustomUserPrincipal;
import server.main.log.orderLog.service.OrderLogService;
import server.main.member.entity.Account;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.MemberRepository;
import server.main.member.repository.MemberTokenHoldingRepository;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.dto.TradeExecutionDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;
import server.main.order.dto.UpdateOrderRequestDto;
import server.main.order.entity.Order;
import server.main.order.entity.OrderDuplicated;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;
import server.main.order.event.OrderWebSocketEvent;
import server.main.order.mapper.OrderMapper;
import server.main.order.repository.OrderDuplicatedRepository;
import server.main.order.repository.OrderRepository;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;
import server.main.trade.entity.SettlementStatus;
import server.main.trade.entity.Trade;
import server.main.trade.entity.TradeDuplicated;
import server.main.trade.repository.TradeDuplicatedRepository;
import server.main.trade.repository.TradeRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderLogService orderLogService;
    private final OrderRepository orderRepository;
    private final TokenRepository tokenRepository;
    private final MemberRepository memberRepository;
    private final MemberTokenHoldingRepository memberTokenHoldingRepository;
    private final AccountRepository accountRepository;
    private final TradeRepository tradeRepository;
    private final BlockchainOutboxService blockchainOutboxService;
    private final OrderDuplicatedRepository orderDuplicatedRepository;
    private final TradeDuplicatedRepository tradeDuplicatedRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final server.main.global.util.MatchClient matchClient;

    // createOrder Phase 1: 검증 + 잔고 차감 + 주문 저장
    @Transactional
    @Override
    public MatchOrderRequestDto validateAndSaveOrder(Long tokenId, OrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();
        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Token findToken = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        // 매수일 경우
        if (OrderType.BUY.equals(dto.getOrderType())) {
            Account findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            long orderAmount = Math.multiplyExact(dto.getOrderPrice(), dto.getOrderQuantity());
            if (findMemberAccount.getAvailableBalance() < orderAmount)
                throw new BusinessException(INSUFFICIENT_BALANCE);
            else
                findMemberAccount.lockBalance(orderAmount);
        }

        // 매도일 경우
        if (OrderType.SELL.equals(dto.getOrderType())) {
            MemberTokenHolding findMemberHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findMember, findToken)
                    .orElseThrow(() -> new BusinessException(INSUFFICIENT_TOKEN_BALANCE));

            if (findMemberHolding.getCurrentQuantity() < dto.getOrderQuantity())
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);

            findMemberHolding.lockQuantity(dto.getOrderQuantity());
        }

        // 주문 생성
        Order createOrder = Order.builder()
                .orderPrice(dto.getOrderPrice())
                .orderQuantity(dto.getOrderQuantity())
                .orderType(dto.getOrderType())
                .orderStatus(OrderStatus.PENDING)
                .filledQuantity(0L)
                .remainingQuantity(dto.getOrderQuantity())
                .orderSequence(null)
                .token(findToken)
                .member(findMember)
                .build();

        orderRepository.save(createOrder);

        // 로그 저장
        String detail = String.format("토큰 ID=%d 매수매도=%s 가격=%d 수량=%d",
                tokenId, dto.getOrderType(), dto.getOrderPrice(), dto.getOrderQuantity());
        orderLogService.save(findMember.getMemberName(), detail, true);

        return MatchOrderRequestDto.builder()
                .tokenId(tokenId)
                .memberId(memberId)
                .orderId(createOrder.getOrderId())
                .orderPrice(createOrder.getOrderPrice())
                .orderQuantity(createOrder.getOrderQuantity())
                .orderType(createOrder.getOrderType())
                .build();
    }

    // createOrder / updateOrder Phase 2: 체결 결과 반영
    @Retryable(retryFor = CannotAcquireLockException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    @Override
    public void processMatchResult(Long orderId, Long tokenId, MatchResultDto matchResult) {
        Order findOrder = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Token findToken = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Member findMember = findOrder.getMember();
        Long memberId = findMember.getMemberId();

        // ORDERS 테이블 업데이트
        long newTotalFilled = findOrder.getFilledQuantity() + matchResult.getFilledQuantity();
        findOrder.applyMatchResult(newTotalFilled, matchResult.getRemainingQuantity(), matchResult.getFinalStatus());
        findOrder.updateSequence(matchResult.getOrderSequence());

        boolean isBuy = OrderType.BUY.equals(findOrder.getOrderType());

        // findMember Account/Holding — 체결 건이 있을 때만 조회
        Account findMemberAccount = null;
        MemberTokenHolding findMemberHolding = null;

        if (!matchResult.getExecutions().isEmpty()) {
            findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            findMemberHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findMember, findToken)
                    .orElse(null);
        }

        Map<Long, Account> counterAccountCache = new HashMap<>();
        Map<Long, MemberTokenHolding> counterHoldingCache = new HashMap<>();

        for (TradeExecutionDto execution : matchResult.getExecutions()) {
            Member counterMember = memberRepository.findById(execution.getCounterMemberId())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            Order counterOrder = orderRepository.findById(execution.getCounterOrderId())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            Trade trade = Trade.builder()
                    .tradePrice(execution.getTradePrice())
                    .tradeQuantity(execution.getTradeQuantity())
                    .totalTradePrice(Math.multiplyExact(execution.getTradePrice(), execution.getTradeQuantity()))
                    .feeAmount(0L)
                    .settlementStatus(SettlementStatus.ON_CHAIN_PENDING)
                    .executedAt(LocalDateTime.now())
                    .token(findToken)
                    .buyer(isBuy ? findMember : counterMember)
                    .seller(isBuy ? counterMember : findMember)
                    .buyOrder(isBuy ? findOrder : counterOrder)
                    .sellOrder(isBuy ? counterOrder : findOrder)
                    .build();

            tradeRepository.save(trade);

            long tradeAmount = Math.multiplyExact(execution.getTradePrice(), execution.getTradeQuantity());

            Account counterAccount = counterAccountCache.get(execution.getCounterMemberId());
            if (counterAccount == null) {
                counterAccount = accountRepository.findWithLockByMember(counterMember)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                counterAccountCache.put(execution.getCounterMemberId(), counterAccount);
            }

            Account buyerAccount  = isBuy ? findMemberAccount : counterAccount;
            Account sellerAccount = isBuy ? counterAccount : findMemberAccount;

            long buyerOrderPrice = isBuy ? findOrder.getOrderPrice() : counterOrder.getOrderPrice();
            long lockedAmount = Math.multiplyExact(buyerOrderPrice, execution.getTradeQuantity());

            buyerAccount.settleBuyTrade(tradeAmount, lockedAmount);
            sellerAccount.settleSellTrade(tradeAmount);

            // 매수자 Holding 반영
            MemberTokenHolding buyerHolding;
            if (isBuy) {
                buyerHolding = findMemberHolding;
            } else {
                buyerHolding = counterHoldingCache.get(execution.getCounterMemberId());
                if (buyerHolding == null) {
                    buyerHolding = memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, findToken)
                            .orElse(null);
                    if (buyerHolding != null) {
                        counterHoldingCache.put(execution.getCounterMemberId(), buyerHolding);
                    }
                }
            }

            if (buyerHolding == null) {
                Member buyer = isBuy ? findMember : counterMember;
                buyerHolding = MemberTokenHolding.createForBuyer(
                        buyer, findToken,
                        execution.getTradeQuantity(),
                        execution.getTradePrice());
                memberTokenHoldingRepository.save(buyerHolding);
                if (isBuy) {
                    findMemberHolding = buyerHolding;
                } else {
                    counterHoldingCache.put(execution.getCounterMemberId(), buyerHolding);
                }
            } else {
                buyerHolding.settleBuyTrade(execution.getTradeQuantity(), execution.getTradePrice());
            }

            // 매도자 Holding 반영
            MemberTokenHolding sellerHolding;
            if (isBuy) {
                sellerHolding = counterHoldingCache.get(execution.getCounterMemberId());
                if (sellerHolding == null) {
                    sellerHolding = memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, findToken)
                            .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                    counterHoldingCache.put(execution.getCounterMemberId(), sellerHolding);
                }
            } else {
                sellerHolding = findMemberHolding;
            }

            if (sellerHolding == null) throw new BusinessException(ENTITY_NOT_FOUNT_ERROR);
            sellerHolding.settleSellTrade(execution.getTradeQuantity());
            blockchainOutboxService.saveTradeOutbox(trade, findToken);

            // 상대방 주문 상태 DB 업데이트
            long newFilledQty = counterOrder.getFilledQuantity() + execution.getTradeQuantity();
            long newRemainingQty = counterOrder.getRemainingQuantity() - execution.getTradeQuantity();
            OrderStatus counterStatus = newRemainingQty == 0 ? OrderStatus.FILLED : OrderStatus.PARTIAL;
            counterOrder.applyMatchResult(newFilledQty, newRemainingQty, counterStatus);

            // trades_duplicated 저장
            tradeDuplicatedRepository.save(TradeDuplicated.builder()
                    .tradeId(trade.getTradeId())
                    .sellerId(trade.getSeller().getMemberId())
                    .buyerId(trade.getBuyer().getMemberId())
                    .sellOrderId(trade.getSellOrder().getOrderId())
                    .buyOrderId(trade.getBuyOrder().getOrderId())
                    .tokenId(tokenId)
                    .tradePrice(trade.getTradePrice())
                    .tradeQuantity(trade.getTradeQuantity())
                    .settlementStatus(trade.getSettlementStatus())
                    .executedAt(trade.getExecutedAt())
                    .feeAmount(trade.getFeeAmount())
                    .totalTradePrice(trade.getTotalTradePrice())
                    .createdAt(LocalDateTime.now())
                    .build());

            // orders_duplicated — 상대방 주문 FILLED 시
            if (counterStatus == OrderStatus.FILLED) {
                orderDuplicatedRepository.save(OrderDuplicated.builder()
                        .orderId(counterOrder.getOrderId())
                        .memberId(execution.getCounterMemberId())
                        .tokenId(tokenId)
                        .orderPrice(counterOrder.getOrderPrice())
                        .orderQuantity(counterOrder.getOrderQuantity())
                        .filledQuantity(newFilledQty)
                        .remainingQuantity(newRemainingQty)
                        .orderType(counterOrder.getOrderType())
                        .orderStatus(counterStatus)
                        .orderSequence(counterOrder.getOrderSequence())
                        .createdAt(counterOrder.getCreatedAt())
                        .updatedAt(counterOrder.getUpdatedAt())
                        .archivedAt(LocalDateTime.now())
                        .build());
            }
        }

        // orders_duplicated — 내 주문 FILLED 시
        if (matchResult.getFinalStatus() == OrderStatus.FILLED) {
            orderDuplicatedRepository.save(OrderDuplicated.builder()
                    .orderId(findOrder.getOrderId())
                    .memberId(memberId)
                    .tokenId(tokenId)
                    .orderPrice(findOrder.getOrderPrice())
                    .orderQuantity(findOrder.getOrderQuantity())
                    .filledQuantity(findOrder.getFilledQuantity())
                    .remainingQuantity(findOrder.getRemainingQuantity())
                    .orderType(findOrder.getOrderType())
                    .orderStatus(findOrder.getOrderStatus())
                    .orderSequence(findOrder.getOrderSequence())
                    .createdAt(findOrder.getCreatedAt())
                    .updatedAt(findOrder.getUpdatedAt())
                    .archivedAt(LocalDateTime.now())
                    .build());
        }

        // WebSocket push 이벤트 발행 — 커밋 후 리스너가 실행
        List<Long> counterMemberIds = matchResult.getExecutions().stream()
                .map(TradeExecutionDto::getCounterMemberId)
                .distinct()
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new OrderWebSocketEvent(tokenId, memberId, counterMemberIds));
    }

    // match 실패 시 보상: 잔고 복구 + 주문 삭제
    @Transactional
    @Override
    public void compensateFailedOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        if (OrderType.BUY.equals(order.getOrderType())) {
            Account account = accountRepository.findWithLockByMember(order.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            long lockedAmount = Math.multiplyExact(order.getOrderPrice(), order.getRemainingQuantity());
            account.cancelOrder(lockedAmount);
        }

        if (OrderType.SELL.equals(order.getOrderType())) {
            MemberTokenHolding holding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(order.getMember(), order.getToken())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            holding.cancelOrder(order.getRemainingQuantity());
        }

        order.removeOrder();
    }

    // updateOrder Phase 1: 검증 + 잔고 재조정 + 주문 수정
    @Transactional
    @Override
    public UpdateMatchOrderRequestDto validateAndUpdateOrder(Long orderId, UpdateOrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        Order findOrder = orderRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        OrderStatus status = findOrder.getOrderStatus();
        if (status != OrderStatus.OPEN && status != OrderStatus.PARTIAL) {
            throw new BusinessException(ORDER_NOT_MODIFIABLE);
        } else if (status == OrderStatus.PARTIAL && dto.getUpdateQuantity() <= findOrder.getFilledQuantity()) {
            throw new BusinessException(INVALID_UPDATE_QUANTITY);
        }

        long newRemaining = dto.getUpdateQuantity() - findOrder.getFilledQuantity();

        if (OrderType.BUY.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findWithLockByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            long oldAmount = Math.multiplyExact(findOrder.getOrderPrice(), findOrder.getRemainingQuantity());
            long updateAmount = Math.multiplyExact(dto.getUpdatePrice(), newRemaining);

            if (findAccount.getAvailableBalance() + oldAmount < updateAmount)
                throw new BusinessException(INSUFFICIENT_BALANCE);
            else
                findAccount.relockBalance(oldAmount, updateAmount);
        }

        if (OrderType.SELL.equals(findOrder.getOrderType())) {
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findOrder.getMember(), findOrder.getToken())
                    .orElseThrow(() -> new BusinessException(INSUFFICIENT_TOKEN_BALANCE));

            long oldQuantity = findOrder.getRemainingQuantity();

            if (tokenHolding.getCurrentQuantity() + oldQuantity < newRemaining) {
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);
            } else {
                tokenHolding.relockQuantity(oldQuantity, newRemaining);
            }
        }

        // 수정 전 값 저장 (보상용) — updateOrder 호출 전에 가져와야 함
        Long originalPrice = findOrder.getOrderPrice();
        Long originalQuantity = findOrder.getOrderQuantity();

        findOrder.updateOrder(dto.getUpdatePrice(), dto.getUpdateQuantity());

        return UpdateMatchOrderRequestDto.builder()
                .orderId(orderId)
                .tokenId(findOrder.getToken().getTokenId())
                .updatePrice(dto.getUpdatePrice())
                .updateQuantity(newRemaining)
                .originalPrice(originalPrice)
                .originalQuantity(originalQuantity)
                .build();
    }

    // update match 실패 시 보상: 원래 가격/수량으로 복구
    @Transactional
    @Override
    public void compensateFailedUpdate(Long orderId, Long originalPrice, Long originalQuantity) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        long newRemaining = originalQuantity - order.getFilledQuantity();

        if (OrderType.BUY.equals(order.getOrderType())) {
            Account account = accountRepository.findWithLockByMember(order.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            long currentLocked = Math.multiplyExact(order.getOrderPrice(), order.getRemainingQuantity());
            long originalLocked = Math.multiplyExact(originalPrice, newRemaining);
            account.relockBalance(currentLocked, originalLocked);
        }

        if (OrderType.SELL.equals(order.getOrderType())) {
            MemberTokenHolding holding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(order.getMember(), order.getToken())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            long currentQuantity = order.getRemainingQuantity();
            holding.relockQuantity(currentQuantity, newRemaining);
        }

        order.restoreOrder(originalPrice, originalQuantity);
    }

    // 미체결 주문 조회
    @Override
    public List<PendingOrderResponseDto> getPendingOrders(Long tokenId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        List<Order> pendingOrders = orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId);
        return orderMapper.toPendingDtoList(pendingOrders);
    }

    // 주문 취소
    @Transactional
    @Override
    public void cancelOrder(Long orderId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        Order findOrder = orderRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        if (!findOrder.getOrderStatus().equals(OrderStatus.OPEN) &&
                !findOrder.getOrderStatus().equals(OrderStatus.PARTIAL)) {
            throw new BusinessException(ORDER_CANNOT_CANCEL);
        }

        if (OrderType.BUY.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findWithLockByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            long lockedAmount = Math.multiplyExact(findOrder.getOrderPrice(), findOrder.getRemainingQuantity());
            findAccount.cancelOrder(lockedAmount);
        }

        if (OrderType.SELL.equals(findOrder.getOrderType())) {
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findOrder.getMember(), findOrder.getToken())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            tokenHolding.cancelOrder(findOrder.getRemainingQuantity());
        }

        findOrder.removeOrder();

        matchClient.cancelOrder(orderId, findOrder.getToken().getTokenId());
    }
}

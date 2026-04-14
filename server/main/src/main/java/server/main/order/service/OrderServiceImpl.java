package server.main.order.service;

import static server.main.global.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.main.admin.entity.PlatformAccount;
import server.main.admin.entity.PlatformAccountType;
import server.main.admin.entity.PlatformBanking;
import server.main.admin.entity.PlatformDirection;
import server.main.admin.entity.Common;
import server.main.admin.repository.CommonRepository;
import server.main.admin.repository.PlatformAccountRepository;
import server.main.admin.repository.PlatformBankingRepository;
import server.main.admin.dto.DashBoardTradeListDTO;
import server.main.admin.event.AdminDashboardEvent;
import server.main.admin.event.TradeExecutedEvent;
import server.main.alarm.entity.AlarmType;
import server.main.alarm.event.AlarmEvent;
import server.main.alarm.service.AlarmService;
import server.main.blockchain.service.BlockchainOutboxService;
import server.main.global.error.BusinessException;
import server.main.global.security.CustomUserPrincipal;
import server.main.global.util.TickSizePolicy;
import server.main.log.orderLog.service.OrderLogService;
import server.main.member.entity.Account;
import server.main.member.entity.Banking;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.member.entity.TxStatus;
import server.main.member.entity.TxType;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.BankingRepository;
import server.main.member.repository.MemberRepository;
import server.main.member.repository.MemberTokenHoldingRepository;
import server.main.order.dto.CancelOrderContext;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.OrderCapacityResponseDto;
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
    private final AlarmService alarmService;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CommonRepository commonRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final BankingRepository bankingRepository;
    private final PlatformBankingRepository platformBankingRepository;

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

        // 호가 단위 검증
        TickSizePolicy.validate(dto.getOrderPrice());

        // 매수일 경우
        if (OrderType.BUY.equals(dto.getOrderType())) {

            Account findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            double chargeRate = getChargeRate();

            long orderAmount = Math.multiplyExact(dto.getOrderPrice(), dto.getOrderQuantity());
            long feeAmount = (long) (orderAmount * (chargeRate / 100));
            long totalLockAmount = orderAmount + feeAmount;

            if (findMemberAccount.getAvailableBalance() < totalLockAmount)
                throw new BusinessException(INSUFFICIENT_BALANCE);
            else
                findMemberAccount.lockBalance(totalLockAmount);
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
        Order findOrder = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Token findToken = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Member findMember = findOrder.getMember();
        Long memberId = findMember.getMemberId();

        // ORDERS 테이블 업데이트 — match 서버는 누적 체결을 모르므로 main 에서 상태 재계산
        long newTotalFilled = findOrder.getFilledQuantity() + matchResult.getFilledQuantity();

        OrderStatus finalStatus;
        if (matchResult.getRemainingQuantity() == 0) {
            finalStatus = OrderStatus.FILLED;
        } else if (newTotalFilled > 0) {
            finalStatus = OrderStatus.PARTIAL;
        } else {
            finalStatus = OrderStatus.OPEN;
        }

        findOrder.applyMatchResult(newTotalFilled, matchResult.getRemainingQuantity(), finalStatus);
        findOrder.updateSequence(matchResult.getOrderSequence());

        boolean isBuy = OrderType.BUY.equals(findOrder.getOrderType());

        // findMember Account/Holding — 체결 건이 있을 때만 조회
        Account findMemberAccount = null;
        MemberTokenHolding findMemberHolding = null;

        Map<Long, Account> counterAccountCache = new HashMap<>();
        Map<Long, MemberTokenHolding> counterHoldingCache = new HashMap<>();

        double chargeRate = 0;
        PlatformAccount platformAccount = null;

        if (!matchResult.getExecutions().isEmpty()) {
            findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            findMemberHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findMember, findToken)
                    .orElse(null);
            chargeRate = getChargeRate();
            platformAccount = platformAccountRepository.findWithLock()
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        }

        for (TradeExecutionDto execution : matchResult.getExecutions()) {
            Member counterMember = memberRepository.findById(execution.getCounterMemberId())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            Order counterOrder = orderRepository.findWithLockById(execution.getCounterOrderId())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            long tradeAmount = Math.multiplyExact(execution.getTradePrice(), execution.getTradeQuantity());
            long feeAmount = (long) (tradeAmount * (chargeRate / 100));

            Trade trade = Trade.builder()
                    .tradePrice(execution.getTradePrice())
                    .tradeQuantity(execution.getTradeQuantity())
                    .totalTradePrice(tradeAmount)
                    .feeAmount(feeAmount) // 매수자 또는 매도자 각 한쪽 기준 수수료 (플랫폼 총 수수료 = feeAmount × 2)
                    .settlementStatus(SettlementStatus.ON_CHAIN_PENDING)
                    .executedAt(LocalDateTime.now())
                    .token(findToken)
                    .buyer(isBuy ? findMember : counterMember)
                    .seller(isBuy ? counterMember : findMember)
                    .buyOrder(isBuy ? findOrder : counterOrder)
                    .sellOrder(isBuy ? counterOrder : findOrder)
                    .build();

            tradeRepository.save(trade);

            // admin 대시보드 이벤트 (거래 체결 시 대시보드 실시간 업데이트)
            eventPublisher.publishEvent(new AdminDashboardEvent());
            // admin 대시보드 실시간 업데이트 용 (체결 거래내역 실시간 업데이트) > 범근
            eventPublisher.publishEvent(new TradeExecutedEvent(
                    DashBoardTradeListDTO.builder()
                            .tradeId(trade.getTradeId())
                            .tradePrice(trade.getTradePrice())
                            .tradeQuantity(trade.getTradeQuantity())
                            .totalTradePrice(trade.getTotalTradePrice())
                            .feeAmount(trade.getFeeAmount())
                            .settlementStatus(String.valueOf(trade.getSettlementStatus()))
                            .executedAt(trade.getExecutedAt())
                            .tokenId(findToken.getTokenId())
                            .tokenName(findToken.getTokenName())
                            .sellerId(trade.getSeller().getMemberId())
                            .sellerName(trade.getSeller().getMemberName())
                            .buyerId(trade.getBuyer().getMemberId())
                            .buyerName(trade.getBuyer().getMemberName())
                            .build()
            ));

            Account counterAccount = counterAccountCache.get(execution.getCounterMemberId());
            if (counterAccount == null) {
                counterAccount = accountRepository.findWithLockByMember(counterMember)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                counterAccountCache.put(execution.getCounterMemberId(), counterAccount);
            }

            Account buyerAccount = isBuy ? findMemberAccount : counterAccount;
            Account sellerAccount = isBuy ? counterAccount : findMemberAccount;

            long buyerOrderPrice = isBuy ? findOrder.getOrderPrice() : counterOrder.getOrderPrice();
            long lockedAmount = Math.multiplyExact(buyerOrderPrice, execution.getTradeQuantity());
            long buyerFeeOnLock = (long) (lockedAmount * (chargeRate / 100));
            long totalLockedAmount = lockedAmount + buyerFeeOnLock;

            buyerAccount.settleBuyTrade(tradeAmount, totalLockedAmount, feeAmount);
            sellerAccount.settleSellTrade(tradeAmount, feeAmount);

            // platform_accounts 수수료 적립 (매수+매도 수수료 합산)
            platformAccount.earnFee(feeAmount * 2);

            // platform_banking 이력 저장
            platformBankingRepository.save(PlatformBanking.builder()
                    .tokenId(findToken.getTokenId())
                    .tradeId(trade.getTradeId())
                    .accountType(PlatformAccountType.FEE)
                    .platformBankingAmount(feeAmount * 2)
                    .platformBankingDirection(PlatformDirection.DEPOSIT)
                    .build());

            // 매수자 거래 이력
            bankingRepository.save(Banking.builder()
                    .account(buyerAccount)
                    .txType(TxType.TRADE_SETTLEMENT)
                    .txStatus(TxStatus.SUCCESS)
                    .bankingAmount(tradeAmount + feeAmount)
                    .balanceSnapshot(buyerAccount.getAvailableBalance())
                    .build());

            // 매도자 거래 이력
            bankingRepository.save(Banking.builder()
                    .account(sellerAccount)
                    .txType(TxType.TRADE_SETTLEMENT)
                    .txStatus(TxStatus.SUCCESS)
                    .bankingAmount(tradeAmount - feeAmount)
                    .balanceSnapshot(sellerAccount.getAvailableBalance())
                    .build());

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

            if (sellerHolding == null)
                throw new BusinessException(ENTITY_NOT_FOUNT_ERROR);
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

        // orders_duplicated — 내 주문 FILLED 시 (재계산된 상태 기준)
        if (findOrder.getOrderStatus() == OrderStatus.FILLED) {
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

        // 주문 체결 시 발생할 알람 리스트
        List<AlarmEvent.AlarmRecord> alarmRecords = new ArrayList<>();
        String tokenName = findToken.getTokenName();

        // 알람 생성 : 주문이 체결되었을 때 알람 생성, FILLED, PARTIAL 구분 // 나와 상대 모두에게 알람 전달
        if ((finalStatus == OrderStatus.FILLED || finalStatus == OrderStatus.PARTIAL)
                && !matchResult.getExecutions().isEmpty()) {
            String orderTypeLabel = isBuy ? "매수" : "매도";
            AlarmType myAlarmType = finalStatus == OrderStatus.FILLED ? AlarmType.ORDER_FILLED
                    : AlarmType.ORDER_PARTIAL; // 부분 체결, 전체 체결 판단
            long totalFilled = matchResult.getExecutions().stream().mapToLong(TradeExecutionDto::getTradeQuantity)
                    .sum();
            long tradePrice = matchResult.getExecutions().get(0).getTradePrice();
            String myMsg = String.format("[ %s ] %s 주문 %,d원 × %d주 %s",
                    tokenName, orderTypeLabel, tradePrice, totalFilled,
                    finalStatus == OrderStatus.FILLED ? "체결 완료" : "부분 체결");

            alarmRecords.add(new AlarmEvent.AlarmRecord(memberId, myAlarmType, tokenId, myMsg));
        }

        // 상대방 알람 생성
        Set<Long> notifiedCounters = new HashSet<>();
        for (TradeExecutionDto execution : matchResult.getExecutions()) { // 매치에서 체결된 내역을 꺼낸다
            Long counterMemberId = execution.getCounterMemberId();
            if (notifiedCounters.contains(counterMemberId))
                continue;

            Order counterOrder = orderRepository.findById(execution.getCounterOrderId()).orElse(null);
            if (counterOrder == null)
                continue;

            OrderStatus counterStatus = counterOrder.getOrderStatus();
            if (counterStatus != OrderStatus.FILLED && counterStatus != OrderStatus.PARTIAL)
                continue;

            String counterTypeLabel = isBuy ? "매도" : "매수"; // 내가 매수자면 상대방은 매도자, 내가 매도자면 상대방은 매수자.
            AlarmType counterAlarmType = counterStatus == OrderStatus.FILLED ? AlarmType.ORDER_FILLED
                    : AlarmType.ORDER_PARTIAL;
            String counterMsg = String.format("[ %s ] %s 주문 %,d원 × %d주 %s",
                    tokenName, counterTypeLabel, execution.getTradePrice(), execution.getTradeQuantity(),
                    counterStatus == OrderStatus.FILLED ? "체결 완료" : "부분 체결");

            alarmRecords.add(new AlarmEvent.AlarmRecord(counterMemberId, counterAlarmType, tokenId, counterMsg));
            notifiedCounters.add(counterMemberId);
        }

        // 이벤트 발생
        if (!alarmRecords.isEmpty()) {
            eventPublisher.publishEvent(new AlarmEvent(alarmRecords));
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
            long feeOnLock = (long) (lockedAmount * (getChargeRate() / 100));
            account.cancelOrder(lockedAmount + feeOnLock);
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

        // 호가 단위 검증
        TickSizePolicy.validate(dto.getUpdatePrice());

        OrderStatus status = findOrder.getOrderStatus();
        if (status != OrderStatus.OPEN && status != OrderStatus.PARTIAL) {
            throw new BusinessException(ORDER_NOT_MODIFIABLE);
        } else if (status == OrderStatus.PARTIAL && dto.getUpdateQuantity() <= findOrder.getFilledQuantity()) {
            throw new BusinessException(INVALID_UPDATE_QUANTITY);
        }

        long newRemaining = dto.getUpdateQuantity() - findOrder.getFilledQuantity();
        double chargeRate = getChargeRate();

        if (OrderType.BUY.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findWithLockByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            long oldAmount = Math.multiplyExact(findOrder.getOrderPrice(), findOrder.getRemainingQuantity());
            long oldFee = (long) (oldAmount * (chargeRate / 100));
            long totalOldAmount = oldAmount + oldFee;

            long updateAmount = Math.multiplyExact(dto.getUpdatePrice(), newRemaining);
            long updateFee = (long) (updateAmount * (chargeRate / 100));
            long totalUpdateAmount = updateAmount + updateFee;

            if (findAccount.getAvailableBalance() + totalOldAmount < totalUpdateAmount)
                throw new BusinessException(INSUFFICIENT_BALANCE);
            else
                findAccount.relockBalance(totalOldAmount, totalUpdateAmount);
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
            double chargeRate = getChargeRate();
            long currentLocked = Math.multiplyExact(order.getOrderPrice(), order.getRemainingQuantity());
            long currentFee = (long) (currentLocked * (chargeRate / 100));
            long originalLocked = Math.multiplyExact(originalPrice, newRemaining);
            long originalFee = (long) (originalLocked * (chargeRate / 100));
            account.relockBalance(currentLocked + currentFee, originalLocked + originalFee);
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

    // cancelOrder Phase 1: 검증 + 잔고 복구 + PENDING 전환
    @Transactional
    @Override
    public CancelOrderContext validateAndCancelOrder(Long orderId) {
        Long memberId = ((CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getId();

        Order findOrder = orderRepository.findWithLockByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        if (!findOrder.getOrderStatus().equals(OrderStatus.OPEN) &&
                !findOrder.getOrderStatus().equals(OrderStatus.PARTIAL)) {
            throw new BusinessException(ORDER_CANNOT_CANCEL);
        }

        OrderStatus originalStatus = findOrder.getOrderStatus();

        if (OrderType.BUY.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findWithLockByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            long lockedAmount = Math.multiplyExact(findOrder.getOrderPrice(), findOrder.getRemainingQuantity());
            long feeOnLock = (long) (lockedAmount * (getChargeRate() / 100));
            findAccount.cancelOrder(lockedAmount + feeOnLock);
        }

        if (OrderType.SELL.equals(findOrder.getOrderType())) {
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findOrder.getMember(), findOrder.getToken())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            tokenHolding.cancelOrder(findOrder.getRemainingQuantity());
        }

        findOrder.markCancelPending();

        return CancelOrderContext.builder()
                .orderId(orderId)
                .tokenId(findOrder.getToken().getTokenId())
                .orderType(findOrder.getOrderType())
                .orderPrice(findOrder.getOrderPrice())
                .remainingQuantity(findOrder.getRemainingQuantity())
                .originalStatus(originalStatus)
                .build();
    }

    // cancelOrder Phase 2: CANCELLED 최종 전환
    @Transactional
    @Override
    public void completeCancelOrder(Long orderId) {
        Order findOrder = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        findOrder.removeOrder();
    }

    // cancel match 실패 시 보상: 잔고 재잠금 + 상태 복원
    @Transactional
    @Override
    public void compensateFailedCancel(CancelOrderContext ctx) {
        Order findOrder = orderRepository.findWithLockById(ctx.getOrderId())
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        if (OrderType.BUY.equals(ctx.getOrderType())) {
            Account findAccount = accountRepository.findWithLockByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            long lockedAmount = Math.multiplyExact(ctx.getOrderPrice(), ctx.getRemainingQuantity());
            long feeOnLock = (long) (lockedAmount * (getChargeRate() / 100));
            findAccount.lockBalance(lockedAmount + feeOnLock);
        }

        if (OrderType.SELL.equals(ctx.getOrderType())) {
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findOrder.getMember(), findOrder.getToken())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            tokenHolding.lockQuantity(ctx.getRemainingQuantity());
        }

        findOrder.restoreOrder(ctx.getOrderPrice(), findOrder.getOrderQuantity());
    }

    // 주문 가능 금액/수량 조회
    @Override
    public OrderCapacityResponseDto getOrderCapacity(Long tokenId) {
        Long memberId = ((CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .getId();
        Long availableBalance = accountRepository.findByMemberId(memberId)
                .map(Account::getAvailableBalance)
                .orElse(0L);
        Long availableQuantity = memberTokenHoldingRepository
                .findByMemberIdAndTokenId(memberId, tokenId)
                .map(MemberTokenHolding::getCurrentQuantity)
                .orElse(0L);
        return new OrderCapacityResponseDto(availableBalance, availableQuantity);
    }

    private double getChargeRate() {
        Common common = commonRepository.findCommon();
        if (common == null) throw new BusinessException(ENTITY_NOT_FOUNT_ERROR);
        return common.getChargeRate();
    }
}

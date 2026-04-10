package server.main.order.service;

import static server.main.global.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.main.blockchain.service.BlockchainOutboxService;
import server.main.global.error.BusinessException;
import server.main.global.security.CustomUserPrincipal;
import server.main.global.util.MatchClient;
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
    private final MatchClient matchClient;
    private final BlockchainOutboxService blockchainOutboxService;
    private final SimpMessagingTemplate messagingTemplate;
    private final OrderDuplicatedRepository orderDuplicatedRepository;
    private final TradeDuplicatedRepository tradeDuplicatedRepository;

    @Transactional
    @Override
    public void createOrder(Long tokenId, OrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();
        Member findMember = memberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Token findToken = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        // findMember Account/Holding — 주문 유형에 따라 조회 후 체결 루프에서 재사용
        Account findMemberAccount = null;
        MemberTokenHolding findMemberHolding = null;

        // 매수일 경우
        if (OrderType.BUY.equals(dto.getOrderType())) {
            // 비관적 락 — 동시 주문 시 잔고 lost update 방지
            findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            // 원화 잔고 >= 총 주문 금액 검증
            long orderAmount = Math.multiplyExact(dto.getOrderPrice(), dto.getOrderQuantity());
            if (findMemberAccount.getAvailableBalance() < orderAmount)
                throw new BusinessException(INSUFFICIENT_BALANCE);

            // 매수 주문일 경우 구매력 차감 (current quantity 감소, locked quantity 증가)
            else
                findMemberAccount.lockBalance(orderAmount); // 더티 체킹
        }

        // 매도일 경우
        if (OrderType.SELL.equals(dto.getOrderType())) {
            // 보유 수량 >= 요청 수량 검증 — 비관적 락으로 동시 초과 매도 방지
            findMemberHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findMember, findToken)
                    .orElseThrow(() -> new BusinessException(INSUFFICIENT_TOKEN_BALANCE));

            if (findMemberHolding.getCurrentQuantity() < dto.getOrderQuantity())
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);

            // 매도 주문일 경우 보유 토큰 감소 (current quantity 감소, locked quantity 증가)
            findMemberHolding.lockQuantity(dto.getOrderQuantity()); // 더티 체킹
        }

        // 주문 생성 (매도, 매수)
        Order createOrder = Order.builder()
                .orderPrice(dto.getOrderPrice())
                .orderQuantity(dto.getOrderQuantity())
                .orderType(dto.getOrderType())
                .orderStatus(OrderStatus.OPEN) // 주문 접수 완료, 아직 매칭 전
                .filledQuantity(0L)
                .remainingQuantity(dto.getOrderQuantity())
                .orderSequence(null) // match 서버가 매칭 시작 시 부여
                .token(findToken)
                .member(findMember)
                .build();

        log.info(String.valueOf(createOrder));

        // 주문 저장
        orderRepository.save(createOrder);

        // 주문을 매치 서버로 전달
        MatchOrderRequestDto matchDto = MatchOrderRequestDto.builder()
                .tokenId(tokenId)
                .memberId(memberId)
                .orderId(createOrder.getOrderId())
                .orderPrice(createOrder.getOrderPrice())
                .orderQuantity(createOrder.getOrderQuantity())
                .orderType(createOrder.getOrderType())
                .build();
        // match 서버 호출 — 실패 시 트랜잭션 전체 롤백
        MatchResultDto matchResult;
        try {
            matchResult = matchClient.sendOrder(matchDto);
        } catch (RestClientException e) {
            log.error("match 서버 호출 실패. orderId={}, tokenId={}", createOrder.getOrderId(), tokenId, e);
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        // ORDERS 테이블 업데이트 (더티 체킹)
        createOrder.applyMatchResult(
                matchResult.getFilledQuantity(),
                matchResult.getRemainingQuantity(),
                matchResult.getFinalStatus());
        createOrder.updateSequence(matchResult.getOrderSequence()); // match가 부여한 시간 우선순위 번호 저장

        // TRADES 테이블 저장 — 체결 건별로 레코드 생성
        boolean isBuy = OrderType.BUY.equals(dto.getOrderType());

        // findMember Account/Holding — 체결 건이 있을 때만 미조회 항목 보완
        if (!matchResult.getExecutions().isEmpty()) {
            if (findMemberAccount == null) {
                // SELL 케이스 — 비관적 락으로 체결 루프 잔고 lost update 방지
                findMemberAccount = accountRepository.findWithLockByMember(findMember)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            }
            if (findMemberHolding == null) {
                // 비관적 락 — 동시 체결 시 같은 (member, token)으로 중복 insert 방지
                findMemberHolding = memberTokenHoldingRepository
                        .findWithLockByMemberAndToken(findMember, findToken)
                        .orElse(null);
            }
        }

        // counterMember Account/Holding — memberId 기준 캐시 (동일인 반복 조회 방지)
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
                    .buyOrder(isBuy ? createOrder : counterOrder)
                    .sellOrder(isBuy ? counterOrder : createOrder)
                    .build();

            tradeRepository.save(trade);

            long tradeAmount = Math.multiplyExact(execution.getTradePrice(), execution.getTradeQuantity());

            // counterMember Account 캐시 조회 — 비관적 락으로 체결 루프 잔고 lost update 방지
            Account counterAccount = counterAccountCache.get(execution.getCounterMemberId());
            if (counterAccount == null) {
                counterAccount = accountRepository.findWithLockByMember(counterMember)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                counterAccountCache.put(execution.getCounterMemberId(), counterAccount);
            }

            Account buyerAccount  = isBuy ? findMemberAccount : counterAccount;
            Account sellerAccount = isBuy ? counterAccount : findMemberAccount;

            // 매수자가 주문 시 잠근 금액 (주문가 기준)
            long buyerOrderPrice = isBuy ? createOrder.getOrderPrice() : counterOrder.getOrderPrice();
            long lockedAmount = Math.multiplyExact(buyerOrderPrice, execution.getTradeQuantity());

            buyerAccount.settleBuyTrade(tradeAmount, lockedAmount); // lockedBalance 차감 + 차액 availableBalance 환급
            sellerAccount.settleSellTrade(tradeAmount);              // 매도자 availableBalance 증가

            // 매수자 Holding 반영
            MemberTokenHolding buyerHolding;
            if (isBuy) {
                buyerHolding = findMemberHolding;
            } else {
                buyerHolding = counterHoldingCache.get(execution.getCounterMemberId());
                if (buyerHolding == null) {
                    // 비관적 락 — 동시 체결 시 같은 (member, token)으로 중복 insert 방지
                    buyerHolding = memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, findToken)
                            .orElse(null);
                    if (buyerHolding != null) {
                        counterHoldingCache.put(execution.getCounterMemberId(), buyerHolding);
                    }
                }
            }

            if (buyerHolding == null) {
                // 처음 받는 토큰 — 새 레코드 생성
                Member buyer = isBuy ? findMember : counterMember;
                buyerHolding = MemberTokenHolding.createForBuyer(
                        buyer, findToken,
                        execution.getTradeQuantity(),
                        execution.getTradePrice());
                memberTokenHoldingRepository.save(buyerHolding); // null 아님 — 바로 위 createForBuyer로 생성, IDE 정적 분석 오탐
                // 캐시 업데이트 — 다음 execution에서 재사용
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
                    // 비관적 락 — 동시 체결 시 lost update 방지
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

            // orders_duplicated — 내 주문 FILLED 시
            if (matchResult.getFinalStatus() == OrderStatus.FILLED) {
                orderDuplicatedRepository.save(OrderDuplicated.builder()
                        .orderId(createOrder.getOrderId())
                        .memberId(memberId)
                        .tokenId(tokenId)
                        .orderPrice(createOrder.getOrderPrice())
                        .orderQuantity(createOrder.getOrderQuantity())
                        .filledQuantity(createOrder.getFilledQuantity())
                        .remainingQuantity(createOrder.getRemainingQuantity())
                        .orderType(createOrder.getOrderType())
                        .orderStatus(createOrder.getOrderStatus())
                        .orderSequence(createOrder.getOrderSequence())
                        .createdAt(createOrder.getCreatedAt())
                        .updatedAt(createOrder.getUpdatedAt())
                        .archivedAt(LocalDateTime.now())
                        .build());
            }

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

        // 체결이 끝나면 웹소켓으로 '대기' 창에 push
        // 현재 사용자 WebSocket push
        messagingTemplate.convertAndSend(
                "/topic/pendingOrders/" + tokenId + "/" + memberId,
                orderMapper.toPendingDtoList(orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId))
        );

        // 상대방 WebSocket push
        matchResult.getExecutions().stream()
                .map(TradeExecutionDto::getCounterMemberId)
                .distinct()
                .forEach(counterMemberId -> messagingTemplate.convertAndSend(
                        "/topic/pendingOrders/" + tokenId + "/" + counterMemberId,
                        orderMapper.toPendingDtoList(orderRepository.findPendingOrderByMemberAndToken(counterMemberId, tokenId))
                ));

        // 로그 저장
        String detail = String.format("토큰 ID=%d 매수매도=%s 가격=%d 수량=%d",
                tokenId, dto.getOrderType(), dto.getOrderPrice(), dto.getOrderQuantity());
        orderLogService.save(findMember.getMemberName(), detail, true); // 트랜잭션 requires new
    }

    // 상세 화면의 '대기' : 소켓 필요
    @Override
    public List<PendingOrderResponseDto> getPendingOrders(Long tokenId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        List<Order> pendingOrders = orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId);
        List<PendingOrderResponseDto> dtos = orderMapper.toPendingDtoList(pendingOrders);
        return dtos;
    }

    @Transactional
    @Override
    public void updateOrder(Long orderId, UpdateOrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        // order 찾기
        Order findOrder = orderRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        // OPEN, PARTIAL 상태값이 아니라면 오류 (PENDING = match 처리 중이므로 수정 불가)
        // PARTIAL 상태일 때만 수량 검증
        OrderStatus status = findOrder.getOrderStatus();
        if (status != OrderStatus.OPEN && status != OrderStatus.PARTIAL) {
            throw new BusinessException(ORDER_NOT_MODIFIABLE);
        } else if (status == OrderStatus.PARTIAL && dto.getUpdateQuantity() <= findOrder.getFilledQuantity()) {
            throw new BusinessException(INVALID_UPDATE_QUANTITY);
        }

        // filledQuantity를 제외한 실제 남은 수량 — match에 전달할 값 (BUY/SELL 공통)
        long newRemaining = dto.getUpdateQuantity() - findOrder.getFilledQuantity();

        // 매수일 경우
        if (OrderType.BUY.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            long oldAmount = findOrder.getOrderPrice() * findOrder.getRemainingQuantity();
            long updateAmount = dto.getUpdatePrice() * newRemaining;

            // 수정 시점 회원의 구매력 < 수정으로 다시 구매할 구매력일 경우 오류 -> 부분 체결일 경우 고려하기
            if (findAccount.getAvailableBalance() + oldAmount < updateAmount)
                throw new BusinessException(INSUFFICIENT_BALANCE);
            else
                findAccount.relockBalance(oldAmount, updateAmount);
        }

        // 매도일 경우
        if (OrderType.SELL.equals(findOrder.getOrderType())) {
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findByMemberAndToken(findOrder.getMember(), findOrder.getToken())
                    .orElseThrow(() -> new BusinessException(INSUFFICIENT_TOKEN_BALANCE));

            long oldQuantity = findOrder.getRemainingQuantity();

            if (tokenHolding.getCurrentQuantity() + oldQuantity < newRemaining) {
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);
            } else {
                tokenHolding.relockQuantity(oldQuantity, newRemaining);
            }
        }

        // DB update (변경 감지)
        findOrder.updateOrder(dto.getUpdatePrice(), dto.getUpdateQuantity());

        // match 전달 — newRemaining: match는 filledQuantity를 모르므로 남은 수량만 전달
        MatchResultDto matchResult;
        try {
            matchResult = matchClient.updateOrder(UpdateMatchOrderRequestDto.builder()
                    .orderId(orderId)
                    .tokenId(findOrder.getToken().getTokenId())
                    .updatePrice(dto.getUpdatePrice())
                    .updateQuantity(newRemaining)
                    .build());
        } catch (RestClientException e) {
            log.error("match 서버 호출 실패. orderId={}, tokenId={}", orderId, findOrder.getToken().getTokenId(), e);
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        // ORDERS 테이블 업데이트 (더티 체킹)
        // filledQuantity: 재매칭으로 새로 체결된 수량을 기존 filledQuantity에 합산
        long newTotalFilled = findOrder.getFilledQuantity() + matchResult.getFilledQuantity();
        findOrder.applyMatchResult(newTotalFilled, matchResult.getRemainingQuantity(), matchResult.getFinalStatus());
        findOrder.updateSequence(matchResult.getOrderSequence());

        // TRADES 테이블 저장 — 체결 건별로 레코드 생성
        boolean isBuy = OrderType.BUY.equals(findOrder.getOrderType());
        Member findMember = findOrder.getMember();
        Token findToken = findOrder.getToken();
        Long tokenId = findToken.getTokenId();

        // findMember Account/Holding — 체결 건이 있을 때만 조회
        Account findMemberAccount = null;
        MemberTokenHolding findMemberHolding = null;

        if (!matchResult.getExecutions().isEmpty()) {
            findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            if (!isBuy) {
                // SELL: 매도자 Holding도 미리 잠금
                findMemberHolding = memberTokenHoldingRepository
                        .findWithLockByMemberAndToken(findMember, findToken)
                        .orElse(null);
            }
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

            // counterMember Account 캐시 조회 — 비관적 락으로 체결 루프 잔고 lost update 방지
            Account counterAccount = counterAccountCache.get(execution.getCounterMemberId());
            if (counterAccount == null) {
                counterAccount = accountRepository.findWithLockByMember(counterMember)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                counterAccountCache.put(execution.getCounterMemberId(), counterAccount);
            }

            Account buyerAccount  = isBuy ? findMemberAccount : counterAccount;
            Account sellerAccount = isBuy ? counterAccount : findMemberAccount;

            // 매수자가 주문 시 잠근 금액 (주문가 기준)
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

        // 체결이 끝나면 웹소켓으로 '대기' 창에 push
        messagingTemplate.convertAndSend(
                "/topic/pendingOrders/" + tokenId + "/" + memberId,
                orderMapper.toPendingDtoList(orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId))
        );

        matchResult.getExecutions().stream()
                .map(TradeExecutionDto::getCounterMemberId)
                .distinct()
                .forEach(counterMemberId -> messagingTemplate.convertAndSend(
                        "/topic/pendingOrders/" + tokenId + "/" + counterMemberId,
                        orderMapper.toPendingDtoList(orderRepository.findPendingOrderByMemberAndToken(counterMemberId, tokenId))
                ));
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        // order 찾기
        Order findOrder = orderRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        // OPEN, PARTIAL 상태값이 아니라면 오류 (PENDING = match 처리 중이므로 취소 불가)
        if (!findOrder.getOrderStatus().equals(OrderStatus.OPEN) &&
                !findOrder.getOrderStatus().equals(OrderStatus.PARTIAL)) {
            throw new BusinessException(ORDER_CANNOT_CANCEL);
        }

        // 매수일 경우
        if (OrderType.BUY.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            // 묶인 금액 풀고 회원 잔고 증가
            Long lockedAmount = findOrder.getOrderPrice() * findOrder.getRemainingQuantity();
            findAccount.cancelOrder(lockedAmount);
        }

        // 매도일 경우
        if (OrderType.SELL.equals(findOrder.getOrderType())) {
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findByMemberAndToken(findOrder.getMember(), findOrder.getToken())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            tokenHolding.cancelOrder(findOrder.getRemainingQuantity()); // 묶인 수량 풀고 회원이 가진 토큰 보유량 증가
        }

        // 주문 삭제 (soft delete)
        findOrder.removeOrder();

        // match 전달 — tokenId는 match가 어느 오더북에서 찾을지 식별하는 데 사용
        matchClient.cancelOrder(orderId, findOrder.getToken().getTokenId());
    }

}

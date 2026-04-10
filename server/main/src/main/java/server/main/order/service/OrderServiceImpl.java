package server.main.order.service;

import static server.main.global.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;
import server.main.order.mapper.OrderMapper;
import server.main.order.repository.OrderRepository;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;
import server.main.trade.entity.SettlementStatus;
import server.main.trade.entity.Trade;
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
            findMemberAccount = accountRepository.findByMember(findMember)
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
            // 보유 수량 >= 요청 수량 검증
            findMemberHolding = memberTokenHoldingRepository
                    .findByMemberAndToken(findMember, findToken)
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

        // TRADES 테이블 저장 — 체결 건별로 레코드 생성
        boolean isBuy = OrderType.BUY.equals(dto.getOrderType());

        // findMember Account/Holding — 체결 건이 있을 때만 미조회 항목 보완
        if (!matchResult.getExecutions().isEmpty()) {
            if (findMemberAccount == null) {
                findMemberAccount = accountRepository.findByMember(findMember)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            }
            if (findMemberHolding == null) {
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

            // counterMember Account 캐시 조회
            Account counterAccount = counterAccountCache.get(execution.getCounterMemberId());
            if (counterAccount == null) {
                counterAccount = accountRepository.findByMember(counterMember)
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
                    sellerHolding = memberTokenHoldingRepository.findByMemberAndToken(counterMember, findToken)
                            .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                    counterHoldingCache.put(execution.getCounterMemberId(), sellerHolding);
                }
            } else {
                sellerHolding = findMemberHolding;
            }

            if (sellerHolding == null) throw new BusinessException(ENTITY_NOT_FOUNT_ERROR);
            sellerHolding.settleSellTrade(execution.getTradeQuantity());
        }

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

        // 매수일 경우
        if (OrderType.BUY.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            long oldAmount = findOrder.getOrderPrice() * findOrder.getRemainingQuantity();
            long newRemaining = dto.getUpdateQuantity() - findOrder.getFilledQuantity();
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
            long updateQuantity = dto.getUpdateQuantity() - findOrder.getFilledQuantity();

            if (tokenHolding.getCurrentQuantity() + oldQuantity < updateQuantity) {
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);
            } else {
                tokenHolding.relockQuantity(oldQuantity, updateQuantity);
            }
        }

        // DB update (변경 감지)
        findOrder.updateOrder(dto.getUpdatePrice(), dto.getUpdateQuantity());

        // match 전달
        matchClient.updateOrder(UpdateMatchOrderRequestDto.builder()
                .orderId(orderId)
                .orderSequence(findOrder.getOrderSequence())
                .updatePrice(dto.getUpdatePrice())
                .updateQuantity(dto.getUpdateQuantity())
                .build());
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

        // match 전달
        matchClient.cancelOrder(orderId);
    }

}

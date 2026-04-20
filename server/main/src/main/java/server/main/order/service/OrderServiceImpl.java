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
import org.springframework.retry.support.RetryTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

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
import server.main.global.util.MatchClient;
import server.main.global.util.TickSizePolicy;
import server.main.log.orderLog.service.OrderLogService;
import server.main.myAccount.entity.Account;
import server.main.member.entity.MemberBank;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.member.entity.TxStatus;
import server.main.member.entity.TxType;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.BankingRepository;
import server.main.member.repository.MemberRepository;
import server.main.member.repository.MemberTokenHoldingRepository;
import server.main.order.dto.CancelOrderContext;
import server.main.order.dto.CancelOrderRequestDto;
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

    private static final int MAX_FAILED_RETRY = 3;

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
    private final PasswordEncoder passwordEncoder;
    private final CommonRepository commonRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final BankingRepository bankingRepository;
    private final PlatformBankingRepository platformBankingRepository;
    private final MatchClient matchClient;
    private final PlatformTransactionManager transactionManager;

    // Validate a match result before applying it to the main database.
    private void validateMatchResult(Order findOrder, Long tokenId, MatchResultDto matchResult) {
        if (matchResult == null
                || matchResult.getOrderId() == null
                || matchResult.getTokenId() == null
                || matchResult.getFilledQuantity() == null
                || matchResult.getRemainingQuantity() == null
                || matchResult.getExecutions() == null) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }

        if (!findOrder.getOrderId().equals(matchResult.getOrderId())
                || !tokenId.equals(matchResult.getTokenId())) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }

        long filledQuantity = matchResult.getFilledQuantity();
        long remainingQuantity = matchResult.getRemainingQuantity();
        long currentFilled = findOrder.getFilledQuantity();
        long orderQuantity = findOrder.getOrderQuantity();
        long availableQuantity = orderQuantity - currentFilled;

        if (filledQuantity < 0 || remainingQuantity < 0 || availableQuantity < 0) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }

        if (currentFilled + filledQuantity > orderQuantity || remainingQuantity > orderQuantity) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }

        long executionTotal = 0L;
        for (TradeExecutionDto execution : matchResult.getExecutions()) {
            validateExecution(execution, availableQuantity);
            executionTotal += execution.getTradeQuantity();
        }

        if (executionTotal != filledQuantity) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }

        if (orderQuantity - (currentFilled + filledQuantity) != remainingQuantity) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }
    }

    private void validateExecution(TradeExecutionDto execution, long availableQuantity) {
        if (execution == null
                || execution.getCounterMemberId() == null
                || execution.getCounterOrderId() == null
                || execution.getTradePrice() == null
                || execution.getTradeQuantity() == null) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }

        if (execution.getTradePrice() <= 0 || execution.getTradeQuantity() <= 0) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }

        if (execution.getTradeQuantity() > availableQuantity) {
            throw new BusinessException(INVALID_INPUT_VALUE);
        }
    }

    private void executeInTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private void executeInRequiresNewTransaction(Runnable action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        template.executeWithoutResult(status -> action.run());
    }

    private <T> T executeInTransactionWithResult(java.util.function.Supplier<T> action) {
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    private <T> T executeInRequiresNewTransactionWithResult(java.util.function.Supplier<T> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template.execute(status -> action.get());
    }

    private RetryTemplate createLockRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .fixedBackoff(100)
                .retryOn(CannotAcquireLockException.class)
                .build();
    }

    private record RetryOrderSnapshot(Long tokenId, MatchResultDto matchResult) {
    }

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

        TickSizePolicy.validate(dto.getOrderPrice());

        if (OrderType.BUY.equals(dto.getOrderType())) {

            Account findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            validateAccountPassword(findMemberAccount, dto.getAccountPassword());

            double chargeRate = getChargeRate();

            long orderAmount = Math.multiplyExact(dto.getOrderPrice(), dto.getOrderQuantity());
            long feeAmount = (long) (orderAmount * (chargeRate / 100));
            long totalLockAmount = orderAmount + feeAmount;

            if (findMemberAccount.getAvailableBalance() < totalLockAmount)
                throw new BusinessException(INSUFFICIENT_BALANCE);
            else
                findMemberAccount.lockBalance(totalLockAmount);
        }

        if (OrderType.SELL.equals(dto.getOrderType())) {
            Account findMemberAccount = accountRepository.findWithLockByMember(findMember)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            validateAccountPassword(findMemberAccount, dto.getAccountPassword());

            MemberTokenHolding findMemberHolding = memberTokenHoldingRepository
                    .findWithLockByMemberAndToken(findMember, findToken)
                    .orElseThrow(() -> new BusinessException(INSUFFICIENT_TOKEN_BALANCE));

            if (findMemberHolding.getCurrentQuantity() < dto.getOrderQuantity())
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);

            findMemberHolding.lockQuantity(dto.getOrderQuantity());
        }

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

        // Save an order log entry.
        String detail = String.format("token=%s price=%d quantity=%d",
                findToken.getTokenName(), dto.getOrderPrice(), dto.getOrderQuantity());
        orderLogService.save(findMember.getMemberName(), String.valueOf(dto.getOrderType()), detail, true);

        return MatchOrderRequestDto.builder()
                .tokenId(tokenId)
                .memberId(memberId)
                .orderId(createOrder.getOrderId())
                .orderPrice(createOrder.getOrderPrice())
                .orderQuantity(createOrder.getOrderQuantity())
                .orderType(createOrder.getOrderType())
                .build();
    }

    // createOrder / updateOrder Phase 2: apply the match result
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
        validateMatchResult(findOrder, tokenId, matchResult);

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
                    .feeAmount(feeAmount) // One-side fee. Platform revenue is feeAmount * 2.
                    .settlementStatus(SettlementStatus.ON_CHAIN_PENDING)
                    .executedAt(LocalDateTime.now())
                    .token(findToken)
                    .buyer(isBuy ? findMember : counterMember)
                    .seller(isBuy ? counterMember : findMember)
                    .buyOrder(isBuy ? findOrder : counterOrder)
                    .sellOrder(isBuy ? counterOrder : findOrder)
                    .build();

            tradeRepository.save(trade);

            findToken.updateCurrentPrice(execution.getTradePrice());

            eventPublisher.publishEvent(new AdminDashboardEvent());
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

            platformAccount.earnFee(feeAmount * 2);

            platformBankingRepository.save(PlatformBanking.builder()
                    .tokenId(findToken.getTokenId())
                    .tradeId(trade.getTradeId())
                    .accountType(PlatformAccountType.FEE)
                    .platformBankingAmount(feeAmount * 2)
                    .platformBankingDirection(PlatformDirection.DEPOSIT)
                    .build());

            bankingRepository.save(MemberBank.builder()
                    .account(buyerAccount)
                    .txType(TxType.TRADE_SETTLEMENT_BUY)
                    .txStatus(TxStatus.SUCCESS)
                    .bankingAmount(tradeAmount + feeAmount)
                    .balanceSnapshot(buyerAccount.getAvailableBalance())
                    .build());

            bankingRepository.save(MemberBank.builder()
                    .account(sellerAccount)
                    .txType(TxType.TRADE_SETTLEMENT_SELL)
                    .txStatus(TxStatus.SUCCESS)
                    .bankingAmount(tradeAmount - feeAmount)
                    .balanceSnapshot(sellerAccount.getAvailableBalance())
                    .build());

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

            long newFilledQty = counterOrder.getFilledQuantity() + execution.getTradeQuantity();
            long newRemainingQty = counterOrder.getRemainingQuantity() - execution.getTradeQuantity();
            if (newFilledQty < 0
                    || newFilledQty > counterOrder.getOrderQuantity()
                    || newRemainingQty < 0
                    || newRemainingQty > counterOrder.getOrderQuantity()) {
                throw new BusinessException(INVALID_INPUT_VALUE);
            }
            OrderStatus counterStatus = newRemainingQty == 0 ? OrderStatus.FILLED : OrderStatus.PARTIAL;
            counterOrder.applyMatchResult(newFilledQty, newRemainingQty, counterStatus);

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

        List<AlarmEvent.AlarmRecord> alarmRecords = new ArrayList<>();
        String tokenName = findToken.getTokenName();

        if ((finalStatus == OrderStatus.FILLED || finalStatus == OrderStatus.PARTIAL)
                && !matchResult.getExecutions().isEmpty()) {
            String orderTypeLabel = isBuy ? "BUY" : "SELL";
            AlarmType myAlarmType = finalStatus == OrderStatus.FILLED ? AlarmType.ORDER_FILLED
                    : AlarmType.ORDER_PARTIAL;
            long totalFilled = matchResult.getExecutions().stream().mapToLong(TradeExecutionDto::getTradeQuantity)
                    .sum();
            long tradePrice = matchResult.getExecutions().get(0).getTradePrice();
            String myMsg = String.format("[ %s ] %s order %,d KRW x %d units %s",
                    tokenName, orderTypeLabel, tradePrice, totalFilled,
                    finalStatus == OrderStatus.FILLED ? "filled" : "partially filled");

            alarmRecords.add(new AlarmEvent.AlarmRecord(memberId, myAlarmType, tokenId, myMsg));
        }

        Set<Long> notifiedCounters = new HashSet<>();
        for (TradeExecutionDto execution : matchResult.getExecutions()) {
            Long counterMemberId = execution.getCounterMemberId();
            if (notifiedCounters.contains(counterMemberId))
                continue;

            Order counterOrder = orderRepository.findById(execution.getCounterOrderId()).orElse(null);
            if (counterOrder == null)
                continue;

            OrderStatus counterStatus = counterOrder.getOrderStatus();
            if (counterStatus != OrderStatus.FILLED && counterStatus != OrderStatus.PARTIAL)
                continue;

            String counterTypeLabel = isBuy ? "SELL" : "BUY";
            AlarmType counterAlarmType = counterStatus == OrderStatus.FILLED ? AlarmType.ORDER_FILLED
                    : AlarmType.ORDER_PARTIAL;
            String counterMsg = String.format("[ %s ] %s order %,d KRW x %d units %s",
                    tokenName, counterTypeLabel, execution.getTradePrice(), execution.getTradeQuantity(),
                    counterStatus == OrderStatus.FILLED ? "filled" : "partially filled");

            alarmRecords.add(new AlarmEvent.AlarmRecord(counterMemberId, counterAlarmType, tokenId, counterMsg));
            notifiedCounters.add(counterMemberId);
        }

        if (!alarmRecords.isEmpty()) {
            eventPublisher.publishEvent(new AlarmEvent(alarmRecords));
        }

        List<Long> counterMemberIds = matchResult.getExecutions().stream()
                .map(TradeExecutionDto::getCounterMemberId)
                .distinct()
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new OrderWebSocketEvent(tokenId, memberId, counterMemberIds));
    }

    @Transactional
    @Override
    public void markOrderFailed(Long orderId, MatchResultDto matchResult) {
        Order order = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        try {
            order.markFailed(objectMapper.writeValueAsString(matchResult));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize match result for retry.", e);
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void retryFailedOrder(Long orderId) {
        RetryOrderSnapshot snapshot = executeInTransactionWithResult(() -> {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

            if (order.getOrderStatus() != OrderStatus.FAILED || order.getRemainingQuantity() <= 0) {
                return null;
            }

            if (order.getFailedMatchResultJson() == null || order.getFailedMatchResultJson().isBlank()) {
                throw new IllegalStateException("Missing stored match result for failed order retry.");
            }

            try {
                MatchResultDto storedMatchResult = objectMapper.readValue(
                        order.getFailedMatchResultJson(),
                        MatchResultDto.class
                );
                return new RetryOrderSnapshot(order.getToken().getTokenId(), storedMatchResult);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deserialize stored match result.", e);
            }
        });

        if (snapshot == null) {
            return;
        }

        try {
            createLockRetryTemplate().execute(context -> {
                executeInTransaction(() -> {
                    Order lockedOrder = orderRepository.findWithLockById(orderId)
                            .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                    if (lockedOrder.getOrderStatus() != OrderStatus.FAILED
                            || lockedOrder.getRemainingQuantity() <= 0) {
                        return;
                    }

                    processMatchResult(lockedOrder.getOrderId(), snapshot.tokenId(), snapshot.matchResult());
                    lockedOrder.resetRetryCount();
                });
                return null;
            });
        } catch (RuntimeException e) {
            boolean shouldCancel = executeInRequiresNewTransactionWithResult(() -> {
                Order lockedOrder = orderRepository.findWithLockById(orderId)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                if (lockedOrder.getOrderStatus() != OrderStatus.FAILED || lockedOrder.getRemainingQuantity() <= 0) {
                    return false;
                }

                lockedOrder.increaseRetryCount();
                return lockedOrder.getRetryCount() >= MAX_FAILED_RETRY;
            });

            if (!shouldCancel) {
                throw e;
            }

            try {
                matchClient.cancelOrder(orderId, snapshot.tokenId());
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound notFound) {
                log.warn("Retry cancel target was not found on match service. orderId={}", orderId, notFound);
            } catch (org.springframework.web.client.RestClientException cancelException) {
                log.error("Failed to cancel order on match service after retry limit. orderId={}", orderId, cancelException);
            }

            executeInRequiresNewTransaction(() -> {
                compensateFailedOrder(orderId);
                log.warn("Failed order retry limit exceeded. Cancelling order. orderId={}", orderId);
            });
        }
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void retryFailedOrders() {
        List<Order> failedOrders = orderRepository.findFailedOrdersForRetry();
        for (Order failedOrder : failedOrders) {
            try {
                retryFailedOrder(failedOrder.getOrderId());
            } catch (RuntimeException e) {
                log.error("Failed order retry failed. orderId={}", failedOrder.getOrderId(), e);
            }
        }
    }

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

    @Transactional
    @Override
    public UpdateMatchOrderRequestDto validateAndUpdateOrder(Long orderId, UpdateOrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        Order findOrder = orderRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

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
            validateAccountPassword(findAccount, dto.getAccountPassword());

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
            Account findAccount = accountRepository.findWithLockByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            validateAccountPassword(findAccount, dto.getAccountPassword());

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

    @Override
    public List<PendingOrderResponseDto> getPendingOrders(Long tokenId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        List<Order> pendingOrders = orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId);
        return orderMapper.toPendingDtoList(pendingOrders);
    }

    @Transactional
    @Override
    public CancelOrderContext validateAndCancelOrder(Long orderId, CancelOrderRequestDto dto) {
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
            validateAccountPassword(findAccount, dto.getAccountPassword());
            long lockedAmount = Math.multiplyExact(findOrder.getOrderPrice(), findOrder.getRemainingQuantity());
            long feeOnLock = (long) (lockedAmount * (getChargeRate() / 100));
            findAccount.cancelOrder(lockedAmount + feeOnLock);
        }

        if (OrderType.SELL.equals(findOrder.getOrderType())) {
            Account findAccount = accountRepository.findWithLockByMember(findOrder.getMember())
                    .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
            validateAccountPassword(findAccount, dto.getAccountPassword());
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

    @Transactional
    @Override
    public void completeCancelOrder(Long orderId) {
        Order findOrder = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        findOrder.removeOrder();
    }

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

    private void validateAccountPassword(Account account, String rawAccountPassword) {
        if (!passwordEncoder.matches(rawAccountPassword, account.getAccountPassword())) {
            throw new BusinessException(INVALID_PASSWORD);
        }
    }
}




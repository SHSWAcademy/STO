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

    // createOrder Phase 1: 寃利?+ ?붽퀬 李④컧 + 二쇰Ц ???
    private void validateMatchResult(Order findOrder, MatchResultDto matchResult) {
        if (matchResult == null
                || matchResult.getFilledQuantity() == null
                || matchResult.getRemainingQuantity() == null
                || matchResult.getExecutions() == null) {
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

        // ?멸? ?⑥쐞 寃利?
        TickSizePolicy.validate(dto.getOrderPrice());

        // 留ㅼ닔??寃쎌슦
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

        // 留ㅻ룄??寃쎌슦
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

        // 二쇰Ц ?앹꽦
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

        // 濡쒓렇 ???
        String detail = String.format("?좏겙=%s 媛寃?%d ?섎웾=%d",
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

    // createOrder / updateOrder Phase 2: 泥닿껐 寃곌낵 諛섏쁺
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
        validateMatchResult(findOrder, matchResult);

        // ORDERS ?뚯씠釉??낅뜲?댄듃 ??match ?쒕쾭???꾩쟻 泥닿껐??紐⑤Ⅴ誘濡?main ?먯꽌 ?곹깭 ?ш퀎??
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

        // findMember Account/Holding ??泥닿껐 嫄댁씠 ?덉쓣 ?뚮쭔 議고쉶
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
                    .feeAmount(feeAmount) // 留ㅼ닔???먮뒗 留ㅻ룄??媛??쒖そ 湲곗? ?섏닔猷?(?뚮옯??珥??섏닔猷?= feeAmount 횞 2)
                    .settlementStatus(SettlementStatus.ON_CHAIN_PENDING)
                    .executedAt(LocalDateTime.now())
                    .token(findToken)
                    .buyer(isBuy ? findMember : counterMember)
                    .seller(isBuy ? counterMember : findMember)
                    .buyOrder(isBuy ? findOrder : counterOrder)
                    .sellOrder(isBuy ? counterOrder : findOrder)
                    .build();

            tradeRepository.save(trade);

            // 泥닿껐媛濡??좏겙 ?꾩옱媛 媛깆떊
            findToken.updateCurrentPrice(execution.getTradePrice());

            // admin ??쒕낫???대깽??(嫄곕옒 泥닿껐 ????쒕낫???ㅼ떆媛??낅뜲?댄듃)
            eventPublisher.publishEvent(new AdminDashboardEvent());
            // admin ??쒕낫???ㅼ떆媛??낅뜲?댄듃 ??(泥닿껐 嫄곕옒?댁뿭 ?ㅼ떆媛??낅뜲?댄듃) > 踰붽렐
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

            // platform_accounts ?섏닔猷??곷┰ (留ㅼ닔+留ㅻ룄 ?섏닔猷??⑹궛)
            platformAccount.earnFee(feeAmount * 2);

            // platform_banking ?대젰 ???
            platformBankingRepository.save(PlatformBanking.builder()
                    .tokenId(findToken.getTokenId())
                    .tradeId(trade.getTradeId())
                    .accountType(PlatformAccountType.FEE)
                    .platformBankingAmount(feeAmount * 2)
                    .platformBankingDirection(PlatformDirection.DEPOSIT)
                    .build());

            // 留ㅼ닔??嫄곕옒 ?대젰
            bankingRepository.save(MemberBank.builder()
                    .account(buyerAccount)
                    .txType(TxType.TRADE_SETTLEMENT_BUY)
                    .txStatus(TxStatus.SUCCESS)
                    .bankingAmount(tradeAmount + feeAmount)
                    .balanceSnapshot(buyerAccount.getAvailableBalance())
                    .build());

            // 留ㅻ룄??嫄곕옒 ?대젰
            bankingRepository.save(MemberBank.builder()
                    .account(sellerAccount)
                    .txType(TxType.TRADE_SETTLEMENT_SELL)
                    .txStatus(TxStatus.SUCCESS)
                    .bankingAmount(tradeAmount - feeAmount)
                    .balanceSnapshot(sellerAccount.getAvailableBalance())
                    .build());

            // 留ㅼ닔??Holding 諛섏쁺
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

            // 留ㅻ룄??Holding 諛섏쁺
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

            // ?곷?諛?二쇰Ц ?곹깭 DB ?낅뜲?댄듃
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

            // trades_duplicated ???
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

            // orders_duplicated ???곷?諛?二쇰Ц FILLED ??
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

        // orders_duplicated ????二쇰Ц FILLED ??(?ш퀎?곕맂 ?곹깭 湲곗?)
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

        // 二쇰Ц 泥닿껐 ??諛쒖깮???뚮엺 由ъ뒪??
        List<AlarmEvent.AlarmRecord> alarmRecords = new ArrayList<>();
        String tokenName = findToken.getTokenName();

        // ?뚮엺 ?앹꽦 : 二쇰Ц??泥닿껐?섏뿀?????뚮엺 ?앹꽦, FILLED, PARTIAL 援щ텇 // ?섏? ?곷? 紐⑤몢?먭쾶 ?뚮엺 ?꾨떖
        if ((finalStatus == OrderStatus.FILLED || finalStatus == OrderStatus.PARTIAL)
                && !matchResult.getExecutions().isEmpty()) {
            String orderTypeLabel = isBuy ? "留ㅼ닔" : "留ㅻ룄";
            AlarmType myAlarmType = finalStatus == OrderStatus.FILLED ? AlarmType.ORDER_FILLED
                    : AlarmType.ORDER_PARTIAL; // 遺遺?泥닿껐, ?꾩껜 泥닿껐 ?먮떒
            long totalFilled = matchResult.getExecutions().stream().mapToLong(TradeExecutionDto::getTradeQuantity)
                    .sum();
            long tradePrice = matchResult.getExecutions().get(0).getTradePrice();
            String myMsg = String.format("[ %s ] %s 二쇰Ц %,d??횞 %d二?%s",
                    tokenName, orderTypeLabel, tradePrice, totalFilled,
                    finalStatus == OrderStatus.FILLED ? "泥닿껐 ?꾨즺" : "遺遺?泥닿껐");

            alarmRecords.add(new AlarmEvent.AlarmRecord(memberId, myAlarmType, tokenId, myMsg));
        }

        // ?곷?諛??뚮엺 ?앹꽦
        Set<Long> notifiedCounters = new HashSet<>();
        for (TradeExecutionDto execution : matchResult.getExecutions()) { // 留ㅼ튂?먯꽌 泥닿껐???댁뿭??爰쇰궦??
            Long counterMemberId = execution.getCounterMemberId();
            if (notifiedCounters.contains(counterMemberId))
                continue;

            Order counterOrder = orderRepository.findById(execution.getCounterOrderId()).orElse(null);
            if (counterOrder == null)
                continue;

            OrderStatus counterStatus = counterOrder.getOrderStatus();
            if (counterStatus != OrderStatus.FILLED && counterStatus != OrderStatus.PARTIAL)
                continue;

            String counterTypeLabel = isBuy ? "留ㅻ룄" : "留ㅼ닔"; // ?닿? 留ㅼ닔?먮㈃ ?곷?諛⑹? 留ㅻ룄?? ?닿? 留ㅻ룄?먮㈃ ?곷?諛⑹? 留ㅼ닔??
            AlarmType counterAlarmType = counterStatus == OrderStatus.FILLED ? AlarmType.ORDER_FILLED
                    : AlarmType.ORDER_PARTIAL;
            String counterMsg = String.format("[ %s ] %s 二쇰Ц %,d??횞 %d二?%s",
                    tokenName, counterTypeLabel, execution.getTradePrice(), execution.getTradeQuantity(),
                    counterStatus == OrderStatus.FILLED ? "泥닿껐 ?꾨즺" : "遺遺?泥닿껐");

            alarmRecords.add(new AlarmEvent.AlarmRecord(counterMemberId, counterAlarmType, tokenId, counterMsg));
            notifiedCounters.add(counterMemberId);
        }

        // ?대깽??諛쒖깮
        if (!alarmRecords.isEmpty()) {
            eventPublisher.publishEvent(new AlarmEvent(alarmRecords));
        }

        // WebSocket push ?대깽??諛쒗뻾 ??而ㅻ컠 ??由ъ뒪?덇? ?ㅽ뻾
        List<Long> counterMemberIds = matchResult.getExecutions().stream()
                .map(TradeExecutionDto::getCounterMemberId)
                .distinct()
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new OrderWebSocketEvent(tokenId, memberId, counterMemberIds));
    }

    // match ?ㅽ뙣 ??蹂댁긽: ?붽퀬 蹂듦뎄 + 二쇰Ц ??젣
    @Transactional
    @Override
    public void markOrderFailed(Long orderId) {
        Order order = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        order.markFailed();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public void retryFailedOrder(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        if (order.getOrderStatus() != OrderStatus.FAILED || order.getRemainingQuantity() <= 0) {
            return;
        }

        Long tokenId = order.getToken().getTokenId();
        MatchOrderRequestDto retryDto = MatchOrderRequestDto.builder()
                .tokenId(tokenId)
                .memberId(order.getMember().getMemberId())
                .orderId(order.getOrderId())
                .orderPrice(order.getOrderPrice())
                .orderQuantity(order.getRemainingQuantity())
                .orderType(order.getOrderType())
                .build();

        try {
            MatchResultDto matchResult = matchClient.sendOrder(retryDto);
            executeInTransaction(() -> {
                Order lockedOrder = orderRepository.findWithLockById(orderId)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                if (lockedOrder.getOrderStatus() != OrderStatus.FAILED || lockedOrder.getRemainingQuantity() <= 0) {
                    return;
                }

                processMatchResult(lockedOrder.getOrderId(), tokenId, matchResult);
                lockedOrder.resetRetryCount();
            });
        } catch (RuntimeException e) {
            executeInRequiresNewTransaction(() -> {
                Order lockedOrder = orderRepository.findWithLockById(orderId)
                        .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
                if (lockedOrder.getOrderStatus() != OrderStatus.FAILED || lockedOrder.getRemainingQuantity() <= 0) {
                    return;
                }

                lockedOrder.increaseRetryCount();
                if (lockedOrder.getRetryCount() >= MAX_FAILED_RETRY) {
                    compensateFailedOrder(lockedOrder.getOrderId());
                    log.warn("Failed order retry limit exceeded. Cancelling order. orderId={}, retryCount={}",
                            lockedOrder.getOrderId(), lockedOrder.getRetryCount());
                }
            });
            throw e;
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

    // updateOrder Phase 1: 寃利?+ ?붽퀬 ?ъ“??+ 二쇰Ц ?섏젙
    @Transactional
    @Override
    public UpdateMatchOrderRequestDto validateAndUpdateOrder(Long orderId, UpdateOrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        Order findOrder = orderRepository.findByMemberIdAndOrderId(memberId, orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        // ?멸? ?⑥쐞 寃利?
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

        // ?섏젙 ??媛????(蹂댁긽?? ??updateOrder ?몄텧 ?꾩뿉 媛?몄?????
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

    // update match ?ㅽ뙣 ??蹂댁긽: ?먮옒 媛寃??섎웾?쇰줈 蹂듦뎄
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

    // 誘몄껜寃?二쇰Ц 議고쉶
    @Override
    public List<PendingOrderResponseDto> getPendingOrders(Long tokenId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long memberId = principal.getId();

        List<Order> pendingOrders = orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId);
        return orderMapper.toPendingDtoList(pendingOrders);
    }

    // cancelOrder Phase 1: 寃利?+ ?붽퀬 蹂듦뎄 + PENDING ?꾪솚
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

    // cancelOrder Phase 2: CANCELLED 理쒖쥌 ?꾪솚
    @Transactional
    @Override
    public void completeCancelOrder(Long orderId) {
        Order findOrder = orderRepository.findWithLockById(orderId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        findOrder.removeOrder();
    }

    // cancel match ?ㅽ뙣 ??蹂댁긽: ?붽퀬 ?ъ옞湲?+ ?곹깭 蹂듭썝
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

    // 二쇰Ц 媛??湲덉븸/?섎웾 議고쉶
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




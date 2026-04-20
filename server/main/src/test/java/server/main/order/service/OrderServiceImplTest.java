package server.main.order.service;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static server.main.global.error.ErrorCode.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import com.fasterxml.jackson.databind.ObjectMapper;

import server.main.admin.entity.Common;
import server.main.admin.entity.PlatformAccount;
import server.main.admin.repository.CommonRepository;
import server.main.admin.repository.PlatformAccountRepository;
import server.main.admin.repository.PlatformBankingRepository;
import server.main.blockchain.service.BlockchainOutboxService;
import server.main.global.error.BusinessException;
import server.main.global.security.CustomUserPrincipal;
import server.main.global.util.MatchClient;
import server.main.log.orderLog.service.OrderLogService;
import server.main.myAccount.entity.Account;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.BankingRepository;
import server.main.member.repository.MemberRepository;
import server.main.member.repository.MemberTokenHoldingRepository;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.OrderCapacityResponseDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.dto.TradeExecutionDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;
import server.main.order.dto.UpdateOrderRequestDto;
import server.main.order.entity.Order;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;
import server.main.order.mapper.OrderMapper;
import server.main.order.repository.OrderDuplicatedRepository;
import server.main.order.repository.OrderRepository;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;
import server.main.trade.repository.TradeDuplicatedRepository;
import server.main.trade.repository.TradeRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock OrderMapper orderMapper;
    @Mock OrderRepository orderRepository;
    @Mock TokenRepository tokenRepository;
    @Mock MemberRepository memberRepository;
    @Mock MemberTokenHoldingRepository memberTokenHoldingRepository;
    @Mock AccountRepository accountRepository;
    @Mock TradeRepository tradeRepository;
    @Mock MatchClient matchClient;
    @Mock OrderLogService orderLogService;
    @Mock BlockchainOutboxService blockchainOutboxService;
    @Mock OrderDuplicatedRepository orderDuplicatedRepository;
    @Mock TradeDuplicatedRepository tradeDuplicatedRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock ObjectMapper objectMapper;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CommonRepository commonRepository;
    @Mock PlatformAccountRepository platformAccountRepository;
    @Mock BankingRepository bankingRepository;
    @Mock PlatformBankingRepository platformBankingRepository;
    @Mock PlatformAccount platformAccount;
    @Mock PlatformTransactionManager transactionManager;

    @InjectMocks
    OrderServiceImpl orderService;

    private final Long MEMBER_ID = 1L;
    private final Long TOKEN_ID = 10L;

    @BeforeEach
    void setSecurityContext() throws Exception {
        CustomUserPrincipal principal = new CustomUserPrincipal(MEMBER_ID, "test-user", "MEMBER", "ROLE_USER");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.setContext(securityContext);

        Common common = mock(Common.class);
        lenient().when(commonRepository.findCommon()).thenReturn(common);
        lenient().when(common.getChargeRate()).thenReturn(0.0);
        lenient().when(passwordEncoder.matches(any(CharSequence.class), nullable(String.class))).thenReturn(true);
        lenient().when(platformAccountRepository.findWithLock()).thenReturn(Optional.of(platformAccount));
        lenient().when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        lenient().when(objectMapper.readValue(anyString(), eq(MatchResultDto.class)))
                .thenAnswer(invocation -> new ObjectMapper().readValue((String) invocation.getArgument(0), MatchResultDto.class));
        lenient().when(objectMapper.writeValueAsString(any(MatchResultDto.class)))
                .thenAnswer(invocation -> new ObjectMapper().writeValueAsString(invocation.getArgument(0)));
    }


    @Test
    void getPendingOrders_미체결주문_정상조회() {
        // given
        Order order = mock(Order.class);
        PendingOrderResponseDto dto = PendingOrderResponseDto.builder()
                .orderId(100L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.OPEN)
                .orderPrice(12000L)
                .orderQuantity(10L)
                .filledQuantity(0L)
                .remainingQuantity(10L)
                .createdAt(LocalDateTime.now())
                .build();

        when(orderRepository.findPendingOrderByMemberAndToken(MEMBER_ID, TOKEN_ID))
                .thenReturn(List.of(order));
        when(orderMapper.toPendingDtoList(List.of(order))).thenReturn(List.of(dto));

        // when
        List<PendingOrderResponseDto> result = orderService.getPendingOrders(TOKEN_ID);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrderId()).isEqualTo(100L);
        assertThat(result.get(0).getOrderStatus()).isEqualTo(OrderStatus.OPEN);
        verify(orderRepository).findPendingOrderByMemberAndToken(MEMBER_ID, TOKEN_ID);
    }

    @Test
    void getPendingOrders_미체결주문없음_빈리스트반환() {
        // given
        when(orderRepository.findPendingOrderByMemberAndToken(MEMBER_ID, TOKEN_ID))
                .thenReturn(List.of());
        when(orderMapper.toPendingDtoList(List.of())).thenReturn(List.of());

        // when
        List<PendingOrderResponseDto> result = orderService.getPendingOrders(TOKEN_ID);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void getPendingOrders_부분체결주문_포함조회() {
        // given
        Order partialOrder = mock(Order.class);
        PendingOrderResponseDto dto = PendingOrderResponseDto.builder()
                .orderId(101L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PARTIAL)
                .orderPrice(12000L)
                .orderQuantity(10L)
                .filledQuantity(4L)
                .remainingQuantity(6L)
                .build();

        when(orderRepository.findPendingOrderByMemberAndToken(MEMBER_ID, TOKEN_ID))
                .thenReturn(List.of(partialOrder));
        when(orderMapper.toPendingDtoList(List.of(partialOrder))).thenReturn(List.of(dto));

        // when
        List<PendingOrderResponseDto> result = orderService.getPendingOrders(TOKEN_ID);

        // then
        assertThat(result.get(0).getOrderStatus()).isEqualTo(OrderStatus.PARTIAL);
        assertThat(result.get(0).getFilledQuantity()).isEqualTo(4L);
        assertThat(result.get(0).getRemainingQuantity()).isEqualTo(6L);
    }


    @Test
    void validateAndSaveOrder_매수_정상접수() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(account.getAccountPassword()).thenReturn("encoded");
        when(passwordEncoder.matches("1234", "encoded")).thenReturn(true);
        when(account.getAvailableBalance()).thenReturn(1_000_000L);

        OrderRequestDto dto = OrderRequestDto.builder().accountPassword("1234")
                .accountPassword("1234")
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        // when
        MatchOrderRequestDto result = orderService.validateAndSaveOrder(TOKEN_ID, dto);

        // then
        verify(orderRepository).save(any(Order.class));
        verify(account).lockBalance(60_000L);
        assertThat(result.getTokenId()).isEqualTo(TOKEN_ID);
        assertThat(result.getMemberId()).isEqualTo(MEMBER_ID);
        assertThat(result.getOrderType()).isEqualTo(OrderType.BUY);
        assertThat(result.getOrderPrice()).isEqualTo(12000L);
        assertThat(result.getOrderQuantity()).isEqualTo(5L);
    }

    @Test
    void validateAndSaveOrder_매수_잔고부족_예외발생() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(10_000L);

        OrderRequestDto dto = OrderRequestDto.builder().accountPassword("1234")
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.validateAndSaveOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validateAndSaveOrder_매도_토큰미보유_예외발생() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.empty());

        OrderRequestDto dto = OrderRequestDto.builder().accountPassword("1234")
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.validateAndSaveOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validateAndSaveOrder_매도_수량부족_예외발생() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(3L);

        OrderRequestDto dto = OrderRequestDto.builder().accountPassword("1234")
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.validateAndSaveOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validateAndSaveOrder_매도_정상접수() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(10L);

        OrderRequestDto dto = OrderRequestDto.builder().accountPassword("1234")
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        // when
        MatchOrderRequestDto result = orderService.validateAndSaveOrder(TOKEN_ID, dto);

        // then
        verify(orderRepository).save(any(Order.class));
        verify(holding).lockQuantity(5L);
        assertThat(result.getTokenId()).isEqualTo(TOKEN_ID);
        assertThat(result.getOrderType()).isEqualTo(OrderType.SELL);
    }


    @Test
    void validateAndUpdateOrder_PENDING상태_수정불가_예외발생() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        when(orderRepository.findByMemberIdAndOrderId(MEMBER_ID, orderId))
                .thenReturn(Optional.of(order));

        UpdateOrderRequestDto dto = UpdateOrderRequestDto.builder()
                .accountPassword("1234")
                .updatePrice(12000L)
                .updateQuantity(5L)
                .build();

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.validateAndUpdateOrder(orderId, dto));
        assertThat(ex.getErrorCode()).isEqualTo(ORDER_NOT_MODIFIABLE);
    }

    @Test
    void validateAndUpdateOrder_PARTIAL상태_수정수량이체결량이하_예외발생() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        when(order.getOrderStatus()).thenReturn(OrderStatus.PARTIAL);
        when(order.getFilledQuantity()).thenReturn(5L);
        when(orderRepository.findByMemberIdAndOrderId(MEMBER_ID, orderId))
                .thenReturn(Optional.of(order));

        UpdateOrderRequestDto dto = UpdateOrderRequestDto.builder()
                .accountPassword("1234")
                .updatePrice(12000L)
                .updateQuantity(5L)
                .build();

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.validateAndUpdateOrder(orderId, dto));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_UPDATE_QUANTITY);
    }

    @Test
    void validateAndUpdateOrder_PARTIAL_BUY_정상수정_남은수량기준_relockBalance검증() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        Account account = mock(Account.class);

        when(order.getOrderStatus()).thenReturn(OrderStatus.PARTIAL);
        when(order.getOrderType()).thenReturn(OrderType.BUY);
        when(order.getFilledQuantity()).thenReturn(5L);
        when(order.getRemainingQuantity()).thenReturn(5L);   // oldAmount = 100 * 5 = 500
        when(order.getOrderPrice()).thenReturn(100L);
        when(order.getOrderQuantity()).thenReturn(10L);
        when(order.getMember()).thenReturn(member);
        when(order.getToken()).thenReturn(token);
        when(token.getTokenId()).thenReturn(TOKEN_ID);
        when(orderRepository.findByMemberIdAndOrderId(MEMBER_ID, orderId)).thenReturn(Optional.of(order));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(0L);  // availableBalance(0) + oldAmount(500) >= updateAmount(360)

        UpdateOrderRequestDto dto = UpdateOrderRequestDto.builder()
                .accountPassword("1234")
                .updatePrice(150L)
                .updateQuantity(8L)  // newRemaining = 8 - 5 = 3, updateAmount = 150 * 3 = 450
                .build();

        // when
        UpdateMatchOrderRequestDto result = orderService.validateAndUpdateOrder(orderId, dto);

        verify(account).relockBalance(500L, 450L);
        assertThat(result.getUpdatePrice()).isEqualTo(150L);
        assertThat(result.getUpdateQuantity()).isEqualTo(3L);
        assertThat(result.getOriginalPrice()).isEqualTo(100L);
        assertThat(result.getOriginalQuantity()).isEqualTo(10L);
    }

    @Test
    void validateAndUpdateOrder_PARTIAL_SELL_정상수정_남은수량기준_relockQuantity검증() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(order.getOrderStatus()).thenReturn(OrderStatus.PARTIAL);
        when(order.getOrderType()).thenReturn(OrderType.SELL);
        when(order.getFilledQuantity()).thenReturn(5L);
        when(order.getRemainingQuantity()).thenReturn(5L);   // oldQuantity = 5
        when(order.getOrderPrice()).thenReturn(100L);
        when(order.getOrderQuantity()).thenReturn(10L);
        when(order.getMember()).thenReturn(member);
        when(order.getToken()).thenReturn(token);
        when(token.getTokenId()).thenReturn(TOKEN_ID);
        when(orderRepository.findByMemberIdAndOrderId(MEMBER_ID, orderId)).thenReturn(Optional.of(order));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(0L);  // currentQuantity(0) + oldQuantity(5) >= newRemaining(3)

        UpdateOrderRequestDto dto = UpdateOrderRequestDto.builder()
                .accountPassword("1234")
                .updatePrice(150L)
                .updateQuantity(8L)  // newRemaining = 8 - 5 = 3
                .build();

        // when
        UpdateMatchOrderRequestDto result = orderService.validateAndUpdateOrder(orderId, dto);

        verify(holding).relockQuantity(5L, 3L);
        assertThat(result.getUpdateQuantity()).isEqualTo(3L);
    }


    @Test
    void validateAndCancelOrder_PENDING상태_취소불가_예외발생() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        when(orderRepository.findWithLockByMemberIdAndOrderId(MEMBER_ID, orderId))
                .thenReturn(Optional.of(order));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.validateAndCancelOrder(orderId,
                        server.main.order.dto.CancelOrderRequestDto.builder().accountPassword("1234").build()));
        assertThat(ex.getErrorCode()).isEqualTo(ORDER_CANNOT_CANCEL);
    }


    @Test
    void processMatchResult_매수_체결_잔고및수량반영() {
        // given
        Long orderId = 1L;
        Long counterMemberId = 2L;
        Long counterOrderId = 99L;

        Member member = mock(Member.class);
        Member counterMember = mock(Member.class);
        Token token = mock(Token.class);
        Account account = mock(Account.class);
        Account counterAccount = mock(Account.class);
        MemberTokenHolding buyerHolding = mock(MemberTokenHolding.class);
        MemberTokenHolding sellerHolding = mock(MemberTokenHolding.class);

        when(member.getMemberId()).thenReturn(MEMBER_ID);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        Order counterOrder = Order.builder()
                .orderId(counterOrderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.SELL)
                .orderStatus(OrderStatus.OPEN)
                .token(token)
                .member(counterMember)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(orderRepository.findWithLockById(counterOrderId)).thenReturn(Optional.of(counterOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberRepository.findById(counterMemberId)).thenReturn(Optional.of(counterMember));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(accountRepository.findWithLockByMember(counterMember)).thenReturn(Optional.of(counterAccount));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(buyerHolding));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, token))
                .thenReturn(Optional.of(sellerHolding));

        TradeExecutionDto execution = TradeExecutionDto.builder()
                .counterMemberId(counterMemberId)
                .counterOrderId(counterOrderId)
                .tradePrice(12000L)
                .tradeQuantity(5L)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.FILLED)
                .filledQuantity(5L)
                .remainingQuantity(0L)
                .executions(List.of(execution))
                .build();

        // when
        orderService.processMatchResult(orderId, TOKEN_ID, matchResult);

        verify(account).settleBuyTrade(60_000L, 60_000L, 0L); // tradeAmount=60000, lockedAmount=12000*5=60000
        verify(counterAccount).settleSellTrade(60_000L, 0L);
        verify(buyerHolding).settleBuyTrade(5L, 12000L);
        verify(sellerHolding).settleSellTrade(5L);
        verify(tradeRepository).save(any());
    }

    @Test
    void processMatchResult_매수_체결가_주문가_차이_차액환급() {
        Long orderId = 1L;
        Long counterMemberId = 2L;
        Long counterOrderId = 99L;

        Member member = mock(Member.class);
        Member counterMember = mock(Member.class);
        Token token = mock(Token.class);
        Account account = mock(Account.class);
        Account counterAccount = mock(Account.class);
        MemberTokenHolding buyerHolding = mock(MemberTokenHolding.class);
        MemberTokenHolding sellerHolding = mock(MemberTokenHolding.class);

        when(member.getMemberId()).thenReturn(MEMBER_ID);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        Order counterOrder = Order.builder()
                .orderId(counterOrderId)
                .orderPrice(10000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.SELL)
                .orderStatus(OrderStatus.OPEN)
                .token(token)
                .member(counterMember)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(orderRepository.findWithLockById(counterOrderId)).thenReturn(Optional.of(counterOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberRepository.findById(counterMemberId)).thenReturn(Optional.of(counterMember));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(accountRepository.findWithLockByMember(counterMember)).thenReturn(Optional.of(counterAccount));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(buyerHolding));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, token))
                .thenReturn(Optional.of(sellerHolding));

        TradeExecutionDto execution = TradeExecutionDto.builder()
                .counterMemberId(counterMemberId)
                .counterOrderId(counterOrderId)
                .tradePrice(10000L)
                .tradeQuantity(5L)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.FILLED)
                .filledQuantity(5L)
                .remainingQuantity(0L)
                .executions(List.of(execution))
                .build();

        // when
        orderService.processMatchResult(orderId, TOKEN_ID, matchResult);

        // then
        // tradeAmount = 10000 * 5 = 50000, lockedAmount = 12000 * 5 = 60000
        verify(account).settleBuyTrade(50_000L, 60_000L, 0L);
        verify(counterAccount).settleSellTrade(50_000L, 0L);
    }

    @Test
    void processMatchResult_매수_처음토큰_보유레코드생성() {
        Long orderId = 1L;
        Long counterMemberId = 2L;
        Long counterOrderId = 99L;

        Member member = mock(Member.class);
        Member counterMember = mock(Member.class);
        Token token = mock(Token.class);
        Account account = mock(Account.class);
        Account counterAccount = mock(Account.class);
        MemberTokenHolding sellerHolding = mock(MemberTokenHolding.class);

        when(member.getMemberId()).thenReturn(MEMBER_ID);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        Order counterOrder = Order.builder()
                .orderId(counterOrderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.SELL)
                .orderStatus(OrderStatus.OPEN)
                .token(token)
                .member(counterMember)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(orderRepository.findWithLockById(counterOrderId)).thenReturn(Optional.of(counterOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberRepository.findById(counterMemberId)).thenReturn(Optional.of(counterMember));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(accountRepository.findWithLockByMember(counterMember)).thenReturn(Optional.of(counterAccount));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.empty());
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, token))
                .thenReturn(Optional.of(sellerHolding));

        TradeExecutionDto execution = TradeExecutionDto.builder()
                .counterMemberId(counterMemberId)
                .counterOrderId(counterOrderId)
                .tradePrice(12000L)
                .tradeQuantity(5L)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.FILLED)
                .filledQuantity(5L)
                .remainingQuantity(0L)
                .executions(List.of(execution))
                .build();

        // when
        orderService.processMatchResult(orderId, TOKEN_ID, matchResult);

        verify(memberTokenHoldingRepository).save(any(MemberTokenHolding.class));
    }


    @Test
    void getOrderCapacity_잔고있고_토큰보유있음_정상반환() {
        // given
        Account account = mock(Account.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(accountRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(500_000L);
        when(memberTokenHoldingRepository.findByMemberIdAndTokenId(MEMBER_ID, TOKEN_ID))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(30L);

        // when
        OrderCapacityResponseDto result = orderService.getOrderCapacity(TOKEN_ID);

        // then
        assertThat(result.getAvailableBalance()).isEqualTo(500_000L);
        assertThat(result.getAvailableQuantity()).isEqualTo(30L);
    }

    @Test
    void getOrderCapacity_Account없음_availableBalance는0() {
        // given
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(accountRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
        when(memberTokenHoldingRepository.findByMemberIdAndTokenId(MEMBER_ID, TOKEN_ID))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(10L);

        // when
        OrderCapacityResponseDto result = orderService.getOrderCapacity(TOKEN_ID);

        // then
        assertThat(result.getAvailableBalance()).isEqualTo(0L);
        assertThat(result.getAvailableQuantity()).isEqualTo(10L);
    }

    @Test
    void getOrderCapacity_토큰미보유_availableQuantity는0() {
        // given
        Account account = mock(Account.class);

        when(accountRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(200_000L);
        when(memberTokenHoldingRepository.findByMemberIdAndTokenId(MEMBER_ID, TOKEN_ID))
                .thenReturn(Optional.empty());

        // when
        OrderCapacityResponseDto result = orderService.getOrderCapacity(TOKEN_ID);

        // then
        assertThat(result.getAvailableBalance()).isEqualTo(200_000L);
        assertThat(result.getAvailableQuantity()).isEqualTo(0L);
    }

    @Test
    void getOrderCapacity_Account없고_토큰미보유_모두0() {
        // given
        when(accountRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.empty());
        when(memberTokenHoldingRepository.findByMemberIdAndTokenId(MEMBER_ID, TOKEN_ID))
                .thenReturn(Optional.empty());

        // when
        OrderCapacityResponseDto result = orderService.getOrderCapacity(TOKEN_ID);

        // then
        assertThat(result.getAvailableBalance()).isEqualTo(0L);
        assertThat(result.getAvailableQuantity()).isEqualTo(0L);
    }

    @Test
    void getOrderCapacity_Member없이_ID로만_쿼리2개만_호출() {
        Account account = mock(Account.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(accountRepository.findByMemberId(MEMBER_ID)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(100_000L);
        when(memberTokenHoldingRepository.findByMemberIdAndTokenId(MEMBER_ID, TOKEN_ID))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(5L);

        // when
        orderService.getOrderCapacity(TOKEN_ID);

        // then
        verify(memberRepository, never()).findById(any());
        verify(tokenRepository, never()).findById(TOKEN_ID);
        verify(accountRepository).findByMemberId(MEMBER_ID);
        verify(memberTokenHoldingRepository).findByMemberIdAndTokenId(MEMBER_ID, TOKEN_ID);
    }

    @Test
    void processMatchResult_매도_체결_잔고및수량반영() {
        Long orderId = 1L;
        Long counterMemberId = 2L;
        Long counterOrderId = 99L;

        Member member = mock(Member.class);
        Member counterMember = mock(Member.class);
        Token token = mock(Token.class);
        Account sellerAccount = mock(Account.class);
        Account counterAccount = mock(Account.class);
        MemberTokenHolding sellerHolding = mock(MemberTokenHolding.class);
        MemberTokenHolding buyerHolding = mock(MemberTokenHolding.class);

        when(member.getMemberId()).thenReturn(MEMBER_ID);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.SELL)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        Order counterOrder = Order.builder()
                .orderId(counterOrderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.OPEN)
                .token(token)
                .member(counterMember)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(orderRepository.findWithLockById(counterOrderId)).thenReturn(Optional.of(counterOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberRepository.findById(counterMemberId)).thenReturn(Optional.of(counterMember));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(sellerAccount));
        when(accountRepository.findWithLockByMember(counterMember)).thenReturn(Optional.of(counterAccount));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(sellerHolding));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, token))
                .thenReturn(Optional.of(buyerHolding));

        TradeExecutionDto execution = TradeExecutionDto.builder()
                .counterMemberId(counterMemberId)
                .counterOrderId(counterOrderId)
                .tradePrice(12000L)
                .tradeQuantity(5L)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.FILLED)
                .filledQuantity(5L)
                .remainingQuantity(0L)
                .executions(List.of(execution))
                .build();

        // when
        orderService.processMatchResult(orderId, TOKEN_ID, matchResult);

        // tradeAmount = 60000, lockedAmount = counterOrder.orderPrice(12000) * 5 = 60000
        verify(counterAccount).settleBuyTrade(60_000L, 60_000L, 0L);
        verify(sellerAccount).settleSellTrade(60_000L, 0L);
        verify(buyerHolding).settleBuyTrade(5L, 12000L);
        verify(sellerHolding).settleSellTrade(5L);
        verify(tradeRepository).save(any());
    }

    @Test
    void processMatchResult_수정후_체결없음_PARTIAL유지_OPEN다운그레이드방지() {
        Long orderId = 1L;

        Member member = mock(Member.class);
        Token token = mock(Token.class);
        when(member.getMemberId()).thenReturn(MEMBER_ID);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(900L)
                .orderQuantity(5L)
                .filledQuantity(3L)
                .remainingQuantity(2L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.OPEN)
                .filledQuantity(0L)
                .remainingQuantity(2L)
                .executions(List.of())
                .build();

        // when
        orderService.processMatchResult(orderId, TOKEN_ID, matchResult);

        assertThat(findOrder.getOrderStatus()).isEqualTo(OrderStatus.PARTIAL);
        assertThat(findOrder.getFilledQuantity()).isEqualTo(3L);  // 3+0=3
        assertThat(findOrder.getRemainingQuantity()).isEqualTo(2L);
    }

    @Test
    void processMatchResult_executionQuantityNull_throwsBusinessException() {
        Long orderId = 1L;

        Member member = mock(Member.class);
        Token token = mock(Token.class);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));

        TradeExecutionDto execution = TradeExecutionDto.builder()
                .counterMemberId(2L)
                .counterOrderId(99L)
                .tradePrice(12000L)
                .tradeQuantity(null)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .filledQuantity(1L)
                .remainingQuantity(4L)
                .executions(List.of(execution))
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.processMatchResult(orderId, TOKEN_ID, matchResult));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_INPUT_VALUE);
    }

    @Test
    void processMatchResult_mismatchedOrderId_throwsBusinessException() {
        Long orderId = 1L;

        Member member = mock(Member.class);
        Token token = mock(Token.class);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(999L)
                .tokenId(TOKEN_ID)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .executions(List.of())
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.processMatchResult(orderId, TOKEN_ID, matchResult));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_INPUT_VALUE);
    }

    @Test
    void processMatchResult_mismatchedTokenId_throwsBusinessException() {
        Long orderId = 1L;

        Member member = mock(Member.class);
        Token token = mock(Token.class);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(999L)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .executions(List.of())
                .build();

        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.processMatchResult(orderId, TOKEN_ID, matchResult));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_INPUT_VALUE);
    }

    @Test
    void processMatchResult_executionQuantityCanExceedFinalRemaining() {
        Long orderId = 1L;
        Long counterMemberId = 2L;
        Long counterOrderId = 99L;

        Member member = mock(Member.class);
        Member counterMember = mock(Member.class);
        Token token = mock(Token.class);
        Account account = mock(Account.class);
        Account counterAccount = mock(Account.class);
        MemberTokenHolding buyerHolding = mock(MemberTokenHolding.class);
        MemberTokenHolding sellerHolding = mock(MemberTokenHolding.class);

        when(member.getMemberId()).thenReturn(MEMBER_ID);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(12000L)
                .orderQuantity(100L)
                .filledQuantity(0L)
                .remainingQuantity(100L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        Order counterOrder = Order.builder()
                .orderId(counterOrderId)
                .orderPrice(12000L)
                .orderQuantity(100L)
                .filledQuantity(0L)
                .remainingQuantity(100L)
                .orderType(OrderType.SELL)
                .orderStatus(OrderStatus.OPEN)
                .token(token)
                .member(counterMember)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(orderRepository.findWithLockById(counterOrderId)).thenReturn(Optional.of(counterOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberRepository.findById(counterMemberId)).thenReturn(Optional.of(counterMember));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(accountRepository.findWithLockByMember(counterMember)).thenReturn(Optional.of(counterAccount));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(buyerHolding));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(counterMember, token))
                .thenReturn(Optional.of(sellerHolding));

        TradeExecutionDto execution = TradeExecutionDto.builder()
                .counterMemberId(counterMemberId)
                .counterOrderId(counterOrderId)
                .tradePrice(12000L)
                .tradeQuantity(60L)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .filledQuantity(60L)
                .remainingQuantity(40L)
                .executions(List.of(execution))
                .build();

        orderService.processMatchResult(orderId, TOKEN_ID, matchResult);

        assertThat(findOrder.getOrderStatus()).isEqualTo(OrderStatus.PARTIAL);
        assertThat(findOrder.getFilledQuantity()).isEqualTo(60L);
        assertThat(findOrder.getRemainingQuantity()).isEqualTo(40L);
    }

    @Test
    void retryFailedOrder_whenMatchFails_increasesRetryCount() {
        Long orderId = 1L;
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        String storedMatchResult = """
                {"orderId":1,"tokenId":10,"filledQuantity":0,"remainingQuantity":3,"executions":[]}
                """;
        when(token.getTokenId()).thenReturn(TOKEN_ID);
        Order order = Order.builder()
                .orderId(orderId)
                .orderPrice(100L)
                .orderQuantity(3L)
                .filledQuantity(0L)
                .remainingQuantity(3L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.FAILED)
                .failedMatchResultJson(storedMatchResult)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(order));

        assertThrows(RuntimeException.class, () -> orderService.retryFailedOrder(orderId));

        assertThat(order.getRetryCount()).isEqualTo(1);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
        verify(matchClient, never()).updateOrder(any());
    }

    @Test
    void retryFailedOrder_whenRetryLimitReached_cancelsOrder() {
        Long orderId = 1L;
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        Account account = mock(Account.class);
        String storedMatchResult = """
                {"orderId":1,"tokenId":10,"filledQuantity":0,"remainingQuantity":2,"executions":[]}
                """;
        when(token.getTokenId()).thenReturn(TOKEN_ID);
        Order order = Order.builder()
                .orderId(orderId)
                .orderPrice(100L)
                .orderQuantity(3L)
                .filledQuantity(0L)
                .remainingQuantity(2L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.FAILED)
                .retryCount(2)
                .failedMatchResultJson(storedMatchResult)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(order));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));

        orderService.retryFailedOrder(orderId);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(matchClient).cancelOrder(orderId, TOKEN_ID);
        verify(account).cancelOrder(200L);
    }

    @Test
    void retryFailedOrder_whenMatchSucceeds_resetsRetryCount() {
        Long orderId = 1L;
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        String storedMatchResult = """
                {"orderId":1,"tokenId":10,"filledQuantity":0,"remainingQuantity":3,"executions":[]}
                """;
        when(token.getTokenId()).thenReturn(TOKEN_ID);

        Order order = Order.builder()
                .orderId(orderId)
                .orderPrice(100L)
                .orderQuantity(3L)
                .filledQuantity(0L)
                .remainingQuantity(2L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.FAILED)
                .retryCount(2)
                .failedMatchResultJson(storedMatchResult)
                .token(token)
                .member(member)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .filledQuantity(0L)
                .remainingQuantity(3L)
                .executions(List.of())
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(order));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(member.getMemberId()).thenReturn(MEMBER_ID);

        orderService.retryFailedOrder(orderId);

        assertThat(order.getRetryCount()).isEqualTo(0);
        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(order.getRemainingQuantity()).isEqualTo(3L);
        verify(matchClient, never()).updateOrder(any());
    }

    @Test
    void retryFailedOrder_whenCancelFailsStillCompensates() {
        Long orderId = 1L;
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        Account account = mock(Account.class);
        String storedMatchResult = """
                {"orderId":1,"tokenId":10,"filledQuantity":0,"remainingQuantity":2,"executions":[]}
                """;
        when(token.getTokenId()).thenReturn(TOKEN_ID);
        Order order = Order.builder()
                .orderId(orderId)
                .orderPrice(100L)
                .orderQuantity(3L)
                .filledQuantity(0L)
                .remainingQuantity(2L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.FAILED)
                .retryCount(2)
                .failedMatchResultJson(storedMatchResult)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(order));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        doThrow(new org.springframework.web.client.RestClientException("down")).when(matchClient).cancelOrder(orderId, TOKEN_ID);

        orderService.retryFailedOrder(orderId);

        assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(account).cancelOrder(200L);
    }
}

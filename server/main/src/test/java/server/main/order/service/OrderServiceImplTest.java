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

    @InjectMocks
    OrderServiceImpl orderService;

    private final Long MEMBER_ID = 1L;
    private final Long TOKEN_ID = 10L;

    @BeforeEach
    void setSecurityContext() {
        CustomUserPrincipal principal = new CustomUserPrincipal(MEMBER_ID, "ㅇㄴㅁㄹㅇ", "MEMBER", "ROLE_USER");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.setContext(securityContext);
    }

    // ──────────────── getPendingOrders ────────────────

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

    // ──────────────── validateAndSaveOrder (Phase 1: 검증 + 잔고 차감 + 주문 저장) ────────────────

    @Test
    void validateAndSaveOrder_매수_정상접수() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findWithLockByMember(member)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(1_000_000L);

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L) // 총 60000 < 잔고 1000000
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
        when(account.getAvailableBalance()).thenReturn(10_000L); // 잔고 부족

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L) // 총 60000 > 잔고 10000
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.validateAndSaveOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validateAndSaveOrder_매도_토큰미보유_예외발생() {
        // given
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.empty());

        OrderRequestDto dto = OrderRequestDto.builder()
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
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(3L); // 보유 3주

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L) // 요청 5주 > 보유 3주
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.validateAndSaveOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
    }

    @Test
    void validateAndSaveOrder_매도_정상접수() {
        // given
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(10L); // 보유 10주

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L) // 요청 5주 <= 보유 10주
                .build();

        // when
        MatchOrderRequestDto result = orderService.validateAndSaveOrder(TOKEN_ID, dto);

        // then
        verify(orderRepository).save(any(Order.class));
        verify(holding).lockQuantity(5L);
        assertThat(result.getTokenId()).isEqualTo(TOKEN_ID);
        assertThat(result.getOrderType()).isEqualTo(OrderType.SELL);
    }

    // ──────────────── validateAndUpdateOrder (Phase 1: 검증 + 잔고 재조정 + 주문 수정) ────────────────

    @Test
    void validateAndUpdateOrder_PENDING상태_수정불가_예외발생() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        when(orderRepository.findByMemberIdAndOrderId(MEMBER_ID, orderId))
                .thenReturn(Optional.of(order));

        UpdateOrderRequestDto dto = UpdateOrderRequestDto.builder()
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
                .updatePrice(12000L)
                .updateQuantity(5L) // filledQuantity(5)와 같음 → 예외
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
                .updatePrice(120L)
                .updateQuantity(8L)  // newRemaining = 8 - 5 = 3, updateAmount = 120 * 3 = 360
                .build();

        // when
        UpdateMatchOrderRequestDto result = orderService.validateAndUpdateOrder(orderId, dto);

        // then: total 기준(120*8=960)이 아닌 remaining 기준(120*3=360)으로 호출되어야 한다
        verify(account).relockBalance(500L, 360L);
        assertThat(result.getUpdatePrice()).isEqualTo(120L);
        assertThat(result.getUpdateQuantity()).isEqualTo(3L); // remaining 기준
        assertThat(result.getOriginalPrice()).isEqualTo(100L);
        assertThat(result.getOriginalQuantity()).isEqualTo(10L);
    }

    @Test
    void validateAndUpdateOrder_PARTIAL_SELL_정상수정_남은수량기준_relockQuantity검증() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
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
        when(memberTokenHoldingRepository.findWithLockByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(0L);  // currentQuantity(0) + oldQuantity(5) >= newRemaining(3)

        UpdateOrderRequestDto dto = UpdateOrderRequestDto.builder()
                .updatePrice(120L)
                .updateQuantity(8L)  // newRemaining = 8 - 5 = 3
                .build();

        // when
        UpdateMatchOrderRequestDto result = orderService.validateAndUpdateOrder(orderId, dto);

        // then: total 기준(8)이 아닌 remaining 기준(3)으로 호출되어야 한다
        verify(holding).relockQuantity(5L, 3L);
        assertThat(result.getUpdateQuantity()).isEqualTo(3L);
    }

    // ──────────────── cancelOrder ────────────────

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
                () -> orderService.validateAndCancelOrder(orderId));
        assertThat(ex.getErrorCode()).isEqualTo(ORDER_CANNOT_CANCEL);
    }

    // ──────────────── processMatchResult (Phase 2: 체결 결과 반영) ────────────────

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

        // 매수 주문 (incoming) — 실제 Order 객체 사용 (applyMatchResult 등 상태 변경이 반영되도록)
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

        // 매도 주문 (상대방 resting order)
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

        // then — orderPrice == tradePrice(12000)이라 차액 없음, lockedAmount == tradeAmount
        verify(account).settleBuyTrade(60_000L, 60_000L); // tradeAmount=60000, lockedAmount=12000*5=60000
        verify(counterAccount).settleSellTrade(60_000L);
        verify(buyerHolding).settleBuyTrade(5L, 12000L);
        verify(sellerHolding).settleSellTrade(5L);
        verify(tradeRepository).save(any());
    }

    @Test
    void processMatchResult_매수_체결가_주문가_차이_차액환급() {
        // given — 매수 주문가(12000) > 체결가(10000) → 차액 환급 검증
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
                .orderPrice(10000L) // 매도 호가 10000 (체결가)
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
                .tradePrice(10000L)  // 체결가 10000 < 주문가 12000
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
        // lockedBalance -= 60000, availableBalance += (60000 - 50000) = 10000 환급
        verify(account).settleBuyTrade(50_000L, 60_000L);
        verify(counterAccount).settleSellTrade(50_000L);
    }

    @Test
    void processMatchResult_매수_처음토큰_보유레코드생성() {
        // given — 매수자가 이 토큰을 처음 받는 상황 (TOKEN_HOLDINGS 레코드 없음)
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
                .thenReturn(Optional.empty()); // 처음 받는 토큰 — 레코드 없음
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

        // then — 새 TOKEN_HOLDINGS 레코드가 save() 되어야 한다
        verify(memberTokenHoldingRepository).save(any(MemberTokenHolding.class));
    }

    // ── getOrderCapacity ────────────────────────────────────────────

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
        // given — memberRepository, tokenRepository는 호출되지 않아야 함
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
        // given — 매도 주문 체결: 매도자는 토큰 차감, 매수자(상대방)는 토큰 지급 + 잔고 차감
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

        // 매도 주문 (incoming)
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

        // 매수 주문 (상대방 resting order)
        Order counterOrder = Order.builder()
                .orderId(counterOrderId)
                .orderPrice(12000L) // resting BUY 주문가 = 체결가
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

        // then — SELL incoming이므로 buyer=counter, seller=incoming
        // tradeAmount = 60000, lockedAmount = counterOrder.orderPrice(12000) * 5 = 60000
        verify(counterAccount).settleBuyTrade(60_000L, 60_000L);
        verify(sellerAccount).settleSellTrade(60_000L);
        verify(buyerHolding).settleBuyTrade(5L, 12000L);
        verify(sellerHolding).settleSellTrade(5L);
        verify(tradeRepository).save(any());
    }

    @Test
    void processMatchResult_수정후_체결없음_PARTIAL유지_OPEN다운그레이드방지() {
        // given — 기존에 5개 중 3개 체결된 PARTIAL 주문이 가격 수정 후 재매칭했지만 체결 0건
        Long orderId = 1L;

        Member member = mock(Member.class);
        Token token = mock(Token.class);
        when(member.getMemberId()).thenReturn(MEMBER_ID);

        Order findOrder = Order.builder()
                .orderId(orderId)
                .orderPrice(900L)        // 수정된 가격
                .orderQuantity(5L)
                .filledQuantity(3L)      // 이전에 3개 체결됨
                .remainingQuantity(2L)
                .orderType(OrderType.BUY)
                .orderStatus(OrderStatus.PENDING)
                .token(token)
                .member(member)
                .build();

        when(orderRepository.findWithLockById(orderId)).thenReturn(Optional.of(findOrder));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));

        // match 서버: 이번 호출에서 체결 0건 → OPEN 반환 (match는 누적을 모름)
        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(orderId)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.OPEN)  // match가 보낸 값 (잘못된 값)
                .filledQuantity(0L)
                .remainingQuantity(2L)
                .executions(List.of())
                .build();

        // when
        orderService.processMatchResult(orderId, TOKEN_ID, matchResult);

        // then — main이 누적 기준으로 재계산하므로 PARTIAL이어야 함 (OPEN 다운그레이드 방지)
        assertThat(findOrder.getOrderStatus()).isEqualTo(OrderStatus.PARTIAL);
        assertThat(findOrder.getFilledQuantity()).isEqualTo(3L);  // 3+0=3
        assertThat(findOrder.getRemainingQuantity()).isEqualTo(2L);
    }
}

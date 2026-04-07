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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import server.main.global.error.BusinessException;
import server.main.global.security.CustomUserPrincipal;
import server.main.global.util.MatchClient;
import server.main.member.entity.Account;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.MemberRepository;
import server.main.member.repository.MemberTokenHoldingRepository;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.dto.UpdateOrderRequestDto;
import server.main.order.entity.Order;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;
import server.main.order.mapper.OrderMapper;
import server.main.order.repository.OrderRepository;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    OrderMapper orderMapper;
    @Mock
    OrderRepository orderRepository;
    @Mock
    TokenRepository tokenRepository;
    @Mock
    MemberRepository memberRepository;
    @Mock
    MemberTokenHoldingRepository memberTokenHoldingRepository;
    @Mock
    AccountRepository accountRepository;
    @Mock
    MatchClient matchClient;

    @InjectMocks
    OrderServiceImpl orderService;

    private final Long MEMBER_ID = 1L;
    private final Long TOKEN_ID = 10L;

    @BeforeEach
    void setSecurityContext() {
        CustomUserPrincipal principal = new CustomUserPrincipal(MEMBER_ID, "ㅇㄴㅁㄹㅇ", "MEMBER", "ROLE_USER");
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(principal);
        SecurityContextHolder.setContext(securityContext);
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
    void createOrder_매수_정상접수() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findByMember(member)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(1_000_000L); // 잔고 세팅

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L) // 총 60000 < 잔고 1000000
                .build();

        // when
        orderService.createOrder(TOKEN_ID, dto);

        // then
        verify(orderRepository).save(any(Order.class));
        verify(matchClient).sendOrder(any());
    }

    @Test
    void createOrder_매수_잔고부족_예외발생() {
        // given
        Account account = mock(Account.class);
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(accountRepository.findByMember(member)).thenReturn(Optional.of(account));
        when(account.getAvailableBalance()).thenReturn(10_000L); // 잔고 부족

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L) // 총 60000 > 잔고 10000
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
        verify(matchClient, never()).sendOrder(any());
    }

    @Test
    void createOrder_매도_토큰미보유_예외발생() {
        // given
        Member member = mock(Member.class);
        Token token = mock(Token.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberTokenHoldingRepository.findByMemberAndToken(member, token))
                .thenReturn(Optional.empty());

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
        verify(matchClient, never()).sendOrder(any());
    }

    @Test
    void createOrder_매도_수량부족_예외발생() {
        // given
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberTokenHoldingRepository.findByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(3L); // 보유 3주

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L) // 요청 5주 > 보유 3주
                .build();

        // when & then
        assertThrows(BusinessException.class,
                () -> orderService.createOrder(TOKEN_ID, dto));
        verify(orderRepository, never()).save(any());
        verify(matchClient, never()).sendOrder(any());
    }

    @Test
    void createOrder_매도_정상접수() {
        // given
        Member member = mock(Member.class);
        Token token = mock(Token.class);
        MemberTokenHolding holding = mock(MemberTokenHolding.class);

        when(memberRepository.findById(MEMBER_ID)).thenReturn(Optional.of(member));
        when(tokenRepository.findById(TOKEN_ID)).thenReturn(Optional.of(token));
        when(memberTokenHoldingRepository.findByMemberAndToken(member, token))
                .thenReturn(Optional.of(holding));
        when(holding.getCurrentQuantity()).thenReturn(10L); // 보유 10주

        OrderRequestDto dto = OrderRequestDto.builder()
                .orderType(OrderType.SELL)
                .orderPrice(12000L)
                .orderQuantity(5L) // 요청 5주 <= 보유 10주
                .build();

        // when
        orderService.createOrder(TOKEN_ID, dto);

        // then
        verify(orderRepository).save(any(Order.class));
        verify(matchClient).sendOrder(any());
    }

    @Test
    void updateOrder_PENDING상태_수정불가_예외발생() {
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
                () -> orderService.updateOrder(orderId, dto));
        assertThat(ex.getErrorCode()).isEqualTo(ORDER_NOT_MODIFIABLE);
        verify(matchClient, never()).updateOrder(any());
    }

    @Test
    void updateOrder_PARTIAL상태_수정수량이체결량이하_예외발생() {
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
                () -> orderService.updateOrder(orderId, dto));
        assertThat(ex.getErrorCode()).isEqualTo(INVALID_UPDATE_QUANTITY);
        verify(matchClient, never()).updateOrder(any());
    }

    @Test
    void cancelOrder_PENDING상태_취소불가_예외발생() {
        // given
        Long orderId = 1L;
        Order order = mock(Order.class);
        when(order.getOrderStatus()).thenReturn(OrderStatus.PENDING);
        when(orderRepository.findByMemberIdAndOrderId(MEMBER_ID, orderId))
                .thenReturn(Optional.of(order));

        // when & then
        BusinessException ex = assertThrows(BusinessException.class,
                () -> orderService.cancelOrder(orderId));
        assertThat(ex.getErrorCode()).isEqualTo(ORDER_CANNOT_CANCEL);
        verify(matchClient, never()).cancelOrder(any());
    }
}

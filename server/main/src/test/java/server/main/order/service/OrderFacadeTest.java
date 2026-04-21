package server.main.order.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClientException;

import server.main.global.error.BusinessException;
import server.main.global.util.MatchClient;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;
import server.main.order.dto.UpdateOrderRequestDto;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;

@ExtendWith(MockitoExtension.class)
class OrderFacadeTest {

    @Mock OrderService orderService;
    @Mock MatchClient matchClient;

    @InjectMocks OrderFacade orderFacade;

    private final Long TOKEN_ID = 10L;
    private final Long ORDER_ID = 1L;

    // ──────────────── createOrder ────────────────

    @Test
    void createOrder_정상_phase1_match_phase2_순서검증() {
        // given
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .accountPassword("1234")
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        MatchOrderRequestDto matchDto = MatchOrderRequestDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .orderType(OrderType.BUY)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.OPEN)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .executions(List.of())
                .build();

        when(orderService.validateAndSaveOrder(TOKEN_ID, requestDto)).thenReturn(matchDto);
        when(matchClient.sendOrder(matchDto)).thenReturn(matchResult);

        // when
        orderFacade.createOrder(TOKEN_ID, requestDto);

        // then — phase1 → match → phase2 순서 검증
        InOrder inOrder = inOrder(orderService, matchClient);
        inOrder.verify(orderService).validateAndSaveOrder(TOKEN_ID, requestDto);
        inOrder.verify(matchClient).sendOrder(matchDto);
        inOrder.verify(orderService).processMatchResult(ORDER_ID, TOKEN_ID, matchResult);
    }

    @Test
    void createOrder_match실패_보상트랜잭션_호출검증() {
        // given
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .accountPassword("1234")
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        MatchOrderRequestDto matchDto = MatchOrderRequestDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .orderType(OrderType.BUY)
                .build();

        when(orderService.validateAndSaveOrder(TOKEN_ID, requestDto)).thenReturn(matchDto);
        when(matchClient.sendOrder(matchDto)).thenThrow(new RestClientException("connection refused"));

        // when & then — BusinessException 발생 + 보상 트랜잭션 호출
        assertThrows(BusinessException.class,
                () -> orderFacade.createOrder(TOKEN_ID, requestDto));

        verify(orderService).compensateFailedOrder(ORDER_ID);
        verify(orderService, never()).processMatchResult(any(), any(), any());
    }

    // ──────────────── updateOrder ────────────────

    @Test
    void updateOrder_정상_phase1_match_phase2_순서검증() {
        // given
        UpdateOrderRequestDto requestDto = UpdateOrderRequestDto.builder()
                .accountPassword("1234")
                .updatePrice(13000L)
                .updateQuantity(8L)
                .build();

        UpdateMatchOrderRequestDto matchDto = UpdateMatchOrderRequestDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .updatePrice(13000L)
                .updateQuantity(3L) // remaining 기준
                .originalPrice(12000L)
                .originalQuantity(5L)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .finalStatus(OrderStatus.OPEN)
                .filledQuantity(0L)
                .remainingQuantity(3L)
                .executions(List.of())
                .build();

        when(orderService.validateAndUpdateOrder(ORDER_ID, requestDto)).thenReturn(matchDto);
        when(matchClient.updateOrder(matchDto)).thenReturn(matchResult);

        // when
        orderFacade.updateOrder(ORDER_ID, requestDto);

        // then
        InOrder inOrder = inOrder(orderService, matchClient);
        inOrder.verify(orderService).validateAndUpdateOrder(ORDER_ID, requestDto);
        inOrder.verify(matchClient).updateOrder(matchDto);
        inOrder.verify(orderService).processMatchResult(ORDER_ID, TOKEN_ID, matchResult);
    }

    @Test
    void updateOrder_match실패_보상트랜잭션_호출검증() {
        // given
        UpdateOrderRequestDto requestDto = UpdateOrderRequestDto.builder()
                .accountPassword("1234")
                .updatePrice(13000L)
                .updateQuantity(8L)
                .build();

        UpdateMatchOrderRequestDto matchDto = UpdateMatchOrderRequestDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .updatePrice(13000L)
                .updateQuantity(3L)
                .originalPrice(12000L)
                .originalQuantity(5L)
                .build();

        when(orderService.validateAndUpdateOrder(ORDER_ID, requestDto)).thenReturn(matchDto);
        when(matchClient.updateOrder(matchDto)).thenThrow(new RestClientException("connection refused"));

        // when & then
        assertThrows(BusinessException.class,
                () -> orderFacade.updateOrder(ORDER_ID, requestDto));

        verify(orderService).compensateFailedUpdate(ORDER_ID, 12000L, 5L);
        verify(orderService, never()).processMatchResult(any(), any(), any());
    }

    @Test
    void createOrder_phase2Failure_storesOriginalMatchResult() {
        OrderRequestDto requestDto = OrderRequestDto.builder()
                .accountPassword("1234")
                .orderType(OrderType.BUY)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .build();

        MatchOrderRequestDto matchDto = MatchOrderRequestDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .orderPrice(12000L)
                .orderQuantity(5L)
                .orderType(OrderType.BUY)
                .build();

        MatchResultDto matchResult = MatchResultDto.builder()
                .orderId(ORDER_ID)
                .tokenId(TOKEN_ID)
                .filledQuantity(0L)
                .remainingQuantity(5L)
                .executions(List.of())
                .build();

        when(orderService.validateAndSaveOrder(TOKEN_ID, requestDto)).thenReturn(matchDto);
        when(matchClient.sendOrder(matchDto)).thenReturn(matchResult);
        doThrow(new RuntimeException("phase2")).when(orderService).processMatchResult(ORDER_ID, TOKEN_ID, matchResult);

        assertThrows(RuntimeException.class, () -> orderFacade.createOrder(TOKEN_ID, requestDto));

        verify(orderService).markOrderFailed(ORDER_ID, matchResult);
    }
}

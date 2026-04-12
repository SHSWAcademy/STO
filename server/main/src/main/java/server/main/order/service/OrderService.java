package server.main.order.service;

import server.main.order.dto.OrderCapacityResponseDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.dto.UpdateOrderRequestDto;

import java.util.List;

public interface OrderService {

    // 매수, 매도 요청 생성, 매치로 전달
    void createOrder(Long tokenId, OrderRequestDto dto);

    // 미체결 주문 조회
    List<PendingOrderResponseDto> getPendingOrders(Long tokenId);

    // 대기창 - 매수, 매도 요청 취소
    void cancelOrder(Long orderId);

    // 대기창 - 매수, 매도 요청 수정
    void updateOrder(Long orderId, UpdateOrderRequestDto dto);

    OrderCapacityResponseDto getOrderCapacity(Long tokenId);
}

package server.main.order.service;

import server.main.order.dto.OrderRequestDto;

import java.util.List;

public interface OrderService {

    // 매수, 매도 요청 생성
    void createOrder(Long tokenId, OrderRequestDto dto);

    // 미체결 주문 조회
    List<?> getPendingOrders(Long tokenId);

    // 매수, 매도 요청 취소
    void cancelOrder(Long orderId);

}

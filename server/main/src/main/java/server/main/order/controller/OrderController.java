package server.main.order.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import server.main.order.dto.OrderCapacityResponseDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.dto.UpdateOrderRequestDto;
import server.main.order.service.OrderFacade;
import server.main.order.service.OrderService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
@Slf4j
public class OrderController {

    private final OrderFacade orderFacade;
    private final OrderService orderService;

    // 매수, 매도 요청
    @PostMapping("/{tokenId}/order")
    public ResponseEntity<Void> order(@PathVariable Long tokenId, @Validated @RequestBody OrderRequestDto dto) {
        orderFacade.createOrder(tokenId, dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }


    // 10% , 25%, 50%, 최대
    @GetMapping("/{tokenId}/order/capacity")
    public ResponseEntity<OrderCapacityResponseDto> getOrderCapacity(@PathVariable Long tokenId) {
        OrderCapacityResponseDto dto = orderService.getOrderCapacity(tokenId);
        return ResponseEntity.ok(dto);
    }

    // 상세 페이지 접근 시 회원 미체결 주문 조회
    @GetMapping("/{tokenId}/order/pending")
    public ResponseEntity<List<PendingOrderResponseDto>> getPendingOrders(@PathVariable Long tokenId) {
        List<PendingOrderResponseDto> dtos = orderService.getPendingOrders(tokenId);
        return ResponseEntity.ok(dtos);
    }

    // 호가 수정
    @PutMapping("/order/update/{orderId}")
    public ResponseEntity<Void> orderUpdate(@PathVariable Long orderId,
            @Validated @RequestBody UpdateOrderRequestDto dto) {
        orderFacade.updateOrder(orderId, dto);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build(); // 204 No Content
    }

    // 호가 삭제 (soft delete)
    @DeleteMapping("/order/cancel/{orderId}")
    public ResponseEntity<Void> orderCancel(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

}

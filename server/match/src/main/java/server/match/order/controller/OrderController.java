package server.match.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.match.order.dto.UpdateMatchOrderRequestDto;
import server.match.order.dto.MatchOrderRequestDto;
import server.match.order.dto.MatchResultDto;
import server.match.order.model.Order;
import server.match.order.model.OrderBook;
import server.match.order.service.MatchingService;
import server.match.order.service.OrderBookRegistry;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderBookRegistry orderBookRegistry;
    private final MatchingService matchingService;

    @PostMapping
    public ResponseEntity<MatchResultDto> order(@RequestBody MatchOrderRequestDto dto) {
        OrderBook orderBook = orderBookRegistry.getOrCreate(dto.getTokenId());

        Order order = new Order(
                dto.getOrderId(),
                dto.getMemberId(),
                dto.getTokenId(),
                dto.getOrderType(),
                dto.getOrderPrice(),
                dto.getOrderQuantity()
        );

        MatchResultDto result = matchingService.match(order, orderBook);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long tokenId) {

        OrderBook orderBook = orderBookRegistry.getOrCreate(tokenId);

        Order order = orderBook.findById(orderId);
        if (order == null) {
            return ResponseEntity.notFound().build(); // 404
        }

        orderBook.removeOrder(order);
        return ResponseEntity.noContent().build(); // 204 — 취소 성공, 반환할 body 없음
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<MatchResultDto> updateOrder(
            @PathVariable Long orderId,
            @RequestBody UpdateMatchOrderRequestDto dto) {

        OrderBook orderBook = orderBookRegistry.getOrCreate(dto.getTokenId());

        Order updatedOrder = orderBook.updateOrder(
                orderId, dto.getUpdatePrice(), dto.getUpdateQuantity());

        // null = orderId가 오더북에 없음 (이미 체결되었거나 취소된 주문)
        if (updatedOrder == null) {
            return ResponseEntity.notFound().build(); // 404
        }

        // 수정 후 즉시 재매칭 — 가격 변경으로 체결 조건이 맞아진 경우 즉시 체결
        MatchResultDto result = matchingService.match(updatedOrder, orderBook);
        return ResponseEntity.ok(result);
    }
}

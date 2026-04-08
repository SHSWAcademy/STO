package server.match.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.match.order.dto.MatchOrderRequestDto;
import server.match.order.dto.MatchResultDto;
import server.match.order.entity.OrderStatus;
import server.match.order.model.Order;
import server.match.order.model.OrderBook;
import server.match.order.service.OrderBookRegistry;

import java.util.List;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderBookRegistry orderBookRegistry;

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

        orderBook.addOrder(order);

        // TODO: 실제 매칭 로직 구현 예정
        MatchResultDto result = MatchResultDto.builder()
                .orderId(dto.getOrderId())
                .tokenId(dto.getTokenId())
                .finalStatus(OrderStatus.OPEN)
                .filledQuantity(0L)
                .remainingQuantity(dto.getOrderQuantity())
                .executions(List.of())
                .build();

        return ResponseEntity.ok(result);
    }
}

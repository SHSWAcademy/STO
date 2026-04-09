package server.match.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
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
}

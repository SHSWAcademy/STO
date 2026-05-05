package server.match.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import server.match.global.redis.RedisPublisher;
import server.match.order.dto.MatchOrderRequestDto;
import server.match.order.dto.MatchResultDto;
import server.match.order.dto.OrderBookEventDto;
import server.match.order.dto.UpdateMatchOrderRequestDto;
import server.match.order.model.Order;
import server.match.order.model.OrderBook;
import server.match.order.service.MatchingService;
import server.match.order.service.OrderBookRegistry;

import java.util.List;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderBookRegistry orderBookRegistry;
    private final MatchingService matchingService;
    private final RedisPublisher redisPublisher;
    private final ObjectMapper objectMapper;

    @PostMapping
    public ResponseEntity<MatchResultDto> order(@RequestBody MatchOrderRequestDto dto) {
        OrderBook orderBook = orderBookRegistry.getOrCreate(dto.getTokenId());

        Order order = new Order(
                dto.getOrderId(),
                dto.getMemberId(),
                dto.getTokenId(),
                dto.getOrderType(),
                dto.getOrderPrice(),
                dto.getOrderQuantity());

        MatchResultDto result = matchingService.match(order, orderBook);

        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{orderId}")
    public ResponseEntity<Void> cancelOrder(
            @PathVariable Long orderId,
            @RequestParam Long tokenId) {

        OrderBook orderBook = orderBookRegistry.getOrCreate(tokenId);

        synchronized (orderBook) {
            Order order = orderBook.findById(orderId);
            if (order == null) {
                return ResponseEntity.notFound().build(); // 404
            }

            orderBook.removeOrder(order);
            redisPublisher.publishOrderBook(orderBook);
        }
        return ResponseEntity.noContent().build(); // 204 — 취소 성공, 반환할 body 없음
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<MatchResultDto> updateOrder(
            @PathVariable Long orderId,
            @RequestBody UpdateMatchOrderRequestDto dto) {

        OrderBook orderBook = orderBookRegistry.getOrCreate(dto.getTokenId());

        synchronized (orderBook) {
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

    @GetMapping("/{tokenId}")
    public ResponseEntity<String> getSnapshot(@PathVariable Long tokenId) throws Exception {
        OrderBook orderBook = orderBookRegistry.getOrCreate(tokenId);
        synchronized (orderBook) {
            List<OrderBookEventDto.PriceLevel> asks = orderBook.getSellOrders().entrySet().stream()
                    .map(e -> OrderBookEventDto.PriceLevel.builder()
                            .price(e.getKey())
                            .quantity(e.getValue().stream().mapToLong(Order::getRemainingQuantity).sum())
                            .build())
                    .toList();
            List<OrderBookEventDto.PriceLevel> bids = orderBook.getBuyOrders().entrySet().stream()
                    .map(e -> OrderBookEventDto.PriceLevel.builder()
                            .price(e.getKey())
                            .quantity(e.getValue().stream().mapToLong(Order::getRemainingQuantity).sum())
                            .build())
                    .toList();
            OrderBookEventDto dto = OrderBookEventDto.builder()
                    .tokenId(tokenId).asks(asks).bids(bids).build();
            return ResponseEntity.ok(objectMapper.writeValueAsString(dto));
        }
    }
}

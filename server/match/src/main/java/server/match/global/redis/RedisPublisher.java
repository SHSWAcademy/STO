package server.match.global.redis;

import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.match.order.dto.OrderBookEventDto;
import server.match.order.dto.TradeEventDto;
import server.match.order.model.Order;
import server.match.order.model.OrderBook;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public void publishTrade(TradeEventDto dto) {
        String channel = "trades:" + dto.getTokenId();
        publish(channel, dto);
    }

    private void publish(String channel, Object dto) {
        try {
            String payload = objectMapper.writeValueAsString(dto);
            redisTemplate.convertAndSend(channel, payload);
        } catch (JsonProcessingException e) {
            log.error("Redis publish 실패 = channel: {}", channel, e);
        }
    }

    public void publishOrderBook(OrderBook orderBook) {
        Long tokenId = orderBook.getTokenId();

        // 매도 호가 집계(asks)
        List<OrderBookEventDto.PriceLevel> asks = orderBook.getSellOrders().entrySet().stream()
            .map(entry -> OrderBookEventDto.PriceLevel.builder()
                .price(entry.getKey())
                .quantity(entry.getValue().stream().mapToLong(Order::getRemainingQuantity).sum())
                .build())
            .toList();

        // 매수 호가 집계 (bids)
        List<OrderBookEventDto.PriceLevel> bids = orderBook.getBuyOrders().entrySet().stream()
            .map(entry -> OrderBookEventDto.PriceLevel.builder()
                .price(entry.getKey())
                .quantity(entry.getValue()
                    .stream()
                    .mapToLong(Order::getRemainingQuantity)
                    .sum())
                .build())
            .toList();
        
        OrderBookEventDto dto = OrderBookEventDto.builder()
            .tokenId(tokenId)
            .asks(asks)
            .bids(bids)
            .build();

        publish("orderBook:" + tokenId, dto);
    }
}

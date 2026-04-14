package server.match.order.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import server.match.global.redis.RedisPublisher;
import server.match.order.dto.MatchResultDto;
import server.match.order.dto.TradeEventDto;
import server.match.order.dto.TradeExecutionDto;
import server.match.order.entity.OrderStatus;
import server.match.order.entity.OrderType;
import server.match.order.model.Order;
import server.match.order.model.OrderBook;

@Service
@RequiredArgsConstructor
public class MatchingService {

    private final RedisPublisher redisPublisher;

    public MatchResultDto match(Order incomingOrder, OrderBook orderBook) {
        List<TradeExecutionDto> executions = new ArrayList<>();
        long filledQuantity = 0L;

        synchronized (orderBook) {
            NavigableMap<Long, Deque<Order>> counterBook = getCounterBook(incomingOrder, orderBook);

            while (incomingOrder.getRemainingQuantity() > 0 && !counterBook.isEmpty()) {
                Map.Entry<Long, Deque<Order>> bestEntry = counterBook.firstEntry();
                long bestPrice = bestEntry.getKey();

                if (!isPriceMatched(incomingOrder, bestPrice)) {
                    break;
                }

                Deque<Order> queue = bestEntry.getValue();
                Order counterOrder = queue.peek();

//                // STP: 자기 자신과는 체결하지 않음
//                if (incomingOrder.getMemberId().equals(counterOrder.getMemberId())) {
//                    break;
//                }

                long tradeQuantity = Math.min(incomingOrder.getRemainingQuantity(), counterOrder.getRemainingQuantity());

                incomingOrder.reduceQuantity(tradeQuantity);
                counterOrder.reduceQuantity(tradeQuantity);
                filledQuantity += tradeQuantity;

                executions.add(TradeExecutionDto.builder()
                        .counterOrderId(counterOrder.getOrderId())
                        .counterMemberId(counterOrder.getMemberId())
                        .tradePrice(bestPrice)
                        .tradeQuantity(tradeQuantity)
                        .build());

                // 체결 1건 발생 → trades 채널로 publish
                redisPublisher.publishTrade(TradeEventDto.builder()
                        .tokenId(incomingOrder.getTokenId())
                        .tradePrice(bestPrice)
                        .tradeQuantity(tradeQuantity)
                        .isBuy(OrderType.BUY.equals(incomingOrder.getOrderType()))
                        .tradeTime(LocalDateTime.now())
                        .build());

                if (counterOrder.getRemainingQuantity() == 0) {
                    orderBook.removeOrder(counterOrder);
                }
            }

            if (incomingOrder.getRemainingQuantity() == 0) {
                // 전량 체결: 이미 오더북에 있던 주문이면 제거 (가격 동일 수정 케이스)
                Order existingOrder = orderBook.findById(incomingOrder.getOrderId());
                if (existingOrder != null) {
                    orderBook.removeOrder(existingOrder);
                }
            } else if (orderBook.findById(incomingOrder.getOrderId()) == null) {
                // 잔량 있고 오더북에 없을 때만 추가
                orderBook.addOrder(incomingOrder);
            }
            redisPublisher.publishOrderBook(orderBook);
        }

        OrderStatus finalStatus;
        if (filledQuantity == 0) {
            finalStatus = OrderStatus.OPEN;
        } else if (incomingOrder.getRemainingQuantity() == 0) {
            finalStatus = OrderStatus.FILLED;
        } else {
            finalStatus = OrderStatus.PARTIAL;
        }

        return MatchResultDto.builder()
                .orderId(incomingOrder.getOrderId())
                .tokenId(incomingOrder.getTokenId())
                .orderSequence(incomingOrder.getSequence()) // FILLED면 null, OPEN/PARTIAL이면 부여된 번호
                .finalStatus(finalStatus)
                .filledQuantity(filledQuantity)
                .remainingQuantity(incomingOrder.getRemainingQuantity())
                .executions(executions)
                .build();
    }

    private NavigableMap<Long, Deque<Order>> getCounterBook(Order order, OrderBook orderBook) {
        return order.getOrderType() == OrderType.BUY
                ? orderBook.getSellOrders()
                : orderBook.getBuyOrders();
    }

    private boolean isPriceMatched(Order incomingOrder, long bestPrice) {
        if (incomingOrder.getOrderType() == OrderType.BUY) {
            return bestPrice <= incomingOrder.getPrice();
        } else {
            return bestPrice >= incomingOrder.getPrice();
        }
    }
}

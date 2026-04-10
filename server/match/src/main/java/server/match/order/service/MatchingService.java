package server.match.order.service;

import org.springframework.stereotype.Service;
import server.match.order.dto.MatchResultDto;
import server.match.order.dto.TradeExecutionDto;
import server.match.order.entity.OrderStatus;
import server.match.order.entity.OrderType;
import server.match.order.model.Order;
import server.match.order.model.OrderBook;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

@Service
public class MatchingService {

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

                if (counterOrder.getRemainingQuantity() == 0) {
                    orderBook.removeOrder(counterOrder);
                }
            }

            if (incomingOrder.getRemainingQuantity() > 0
                    && orderBook.findById(incomingOrder.getOrderId()) == null) {
                orderBook.addOrder(incomingOrder);
            }
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

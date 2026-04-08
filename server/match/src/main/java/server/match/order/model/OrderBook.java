package server.match.order.model;

import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import server.match.order.entity.OrderType;

public class OrderBook {

    private final Long tokenId;

    // 매수 주문: 높은 가격 우선 (내림차순)
    private final TreeMap<Long, Deque<Order>> buyOrders = new TreeMap<>(Comparator.reverseOrder());

    // 매도 주문: 낮은 가격 우선 (오름차순)
    private final TreeMap<Long, Deque<Order>> sellOrders = new TreeMap<>();

    // orderId → Order 빠른 조회 (수정/취소 대비)
    private final Map<Long, Order> orderIndex = new HashMap<>();

    public OrderBook(Long tokenId) {
        this.tokenId = tokenId;
    }

    public void addOrder(Order order) {
        TreeMap<Long, Deque<Order>> book = getBook(order.getOrderType());
        book.computeIfAbsent(order.getPrice(), price -> new ArrayDeque<>()).add(order);
        orderIndex.put(order.getOrderId(), order);
    }

    public void removeOrder(Order order) {
        TreeMap<Long, Deque<Order>> book = getBook(order.getOrderType());
        Deque<Order> queue = book.get(order.getPrice());
        if (queue != null) {
            queue.remove(order);
            if (queue.isEmpty()) {
                book.remove(order.getPrice());
            }
        }
        orderIndex.remove(order.getOrderId());
    }

    public Order findById(Long orderId) {
        return orderIndex.get(orderId);
    }

    public TreeMap<Long, Deque<Order>> getBuyOrders() {
        return buyOrders;
    }

    public TreeMap<Long, Deque<Order>> getSellOrders() {
        return sellOrders;
    }

    public Long getTokenId() {
        return tokenId;
    }

    private TreeMap<Long, Deque<Order>> getBook(OrderType orderType) {
        return orderType == OrderType.BUY ? buyOrders : sellOrders;
    }
}

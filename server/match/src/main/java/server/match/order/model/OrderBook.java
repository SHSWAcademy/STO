package server.match.order.model;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;

import server.match.order.entity.OrderType;

public class OrderBook {

    private final Long tokenId;

    // 매수 주문: 높은 가격 우선 (내림차순)
    private final TreeMap<Long, Deque<Order>> buyOrders = new TreeMap<>(Comparator.reverseOrder());

    // 매도 주문: 낮은 가격 우선 (오름차순)
    private final TreeMap<Long, Deque<Order>> sellOrders = new TreeMap<>();

    // orderId → Order 빠른 조회 (수정/취소 대비)
    private final Map<Long, Order> orderIndex = new HashMap<>();

    // 오더북 삽입 순서 카운터 — 시간 우선순위 번호 부여용
    private final AtomicLong sequenceCounter = new AtomicLong(0);

    public OrderBook(Long tokenId) {
        this.tokenId = tokenId;
    }

    public synchronized void addOrder(Order order) {
        order.assignSequence(sequenceCounter.incrementAndGet()); // 시간 우선순위 번호 부여
        TreeMap<Long, Deque<Order>> book = getBook(order.getOrderType());
        book.computeIfAbsent(order.getPrice(), price -> new ArrayDeque<>()).add(order);
        orderIndex.put(order.getOrderId(), order);
    }

    public synchronized void removeOrder(Order order) {
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

    public synchronized Order findById(Long orderId) {
        return orderIndex.get(orderId);
    }

    // 가격 변경: remove + re-add (시간 우선순위 초기화)
    // 수량 감소: in-place 수정 (시간 우선순위 유지)
    public synchronized Order updateOrder(Long orderId, Long newPrice, Long newQuantity) {
        Order old = orderIndex.get(orderId);
        if (old == null) return null;

        if (old.getPrice().equals(newPrice)) {
            // 가격 동일 → 수량만 변경, 줄 위치 유지
            old.updateQuantity(newQuantity);
            return old;
        } else {
            // 가격 변경 → 기존 주문 제거, 새 주문 반환 (sequence는 re-add 시 부여)
            removeOrder(old);
            return new Order(old.getOrderId(), old.getMemberId(),
                    old.getTokenId(), old.getOrderType(), newPrice, newQuantity);
        }
    }

    public NavigableMap<Long, Deque<Order>> getBuyOrders() {
        return Collections.unmodifiableNavigableMap(buyOrders);
    }

    public NavigableMap<Long, Deque<Order>> getSellOrders() {
        return Collections.unmodifiableNavigableMap(sellOrders);
    }

    public Long getTokenId() {
        return tokenId;
    }

    private TreeMap<Long, Deque<Order>> getBook(OrderType orderType) {
        return orderType == OrderType.BUY ? buyOrders : sellOrders;
    }
}

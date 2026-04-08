package server.match.order.service;

import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import server.match.order.model.OrderBook;

@Component
public class OrderBookRegistry {

    private final ConcurrentHashMap<Long, OrderBook> orderBooks = new ConcurrentHashMap<>();

    public OrderBook getOrCreate(Long tokenId) {
        return orderBooks.computeIfAbsent(tokenId, OrderBook::new);
    }
}

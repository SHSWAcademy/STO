package server.main.global.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import server.main.order.entity.Order;
import server.main.order.entity.OrderType;
import server.main.order.repository.OrderRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Component
@RequiredArgsConstructor
public class OrderBookSubscribeHandler {

    private final SimpMessagingTemplate template;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {

        // match 서버 (OrderBookInitializer) — 서버 시작 시 DB 조회 -> 매치 in-memory에 적재
        // main 서버 (OrderBookSubscribeHandler) — 상세 페이지 접근 시 DB 조회 -> 화면에 스냅샷 전송
        // 각각 DB 조회

        // 상세 페이지 접근 시 처음 DB 에서 조회하는 호가창 정보
        String destination = (String) event.getMessage().getHeaders()
                .get(SimpMessageHeaderAccessor.DESTINATION_HEADER);

        if (destination == null || !destination.startsWith("/topic/orderBook/")) return;

        Long tokenId = Long.parseLong(destination.replace("/topic/orderBook/", ""));

        // DB에서 OPEN/PARTIAL 주문 조회 → 가격별 잔량 집계
        List<Order> orders = orderRepository.findOpenAndPartialByTokenId(tokenId);

        Map<Long, Long> askMap = new TreeMap<>();                          // 매도: 낮은 가격 우선
        Map<Long, Long> bidMap = new TreeMap<>(Comparator.reverseOrder()); // 매수: 높은 가격 우선

        for (Order o : orders) {
            if (o.getOrderType() == OrderType.SELL) {
                askMap.merge(o.getOrderPrice(), o.getRemainingQuantity(), Long::sum);
            } else {
                bidMap.merge(o.getOrderPrice(), o.getRemainingQuantity(), Long::sum);
            }
        }

        List<Map<String, Long>> asks = askMap.entrySet().stream()
                .map(e -> Map.of("price", e.getKey(), "quantity", e.getValue()))
                .toList();

        List<Map<String, Long>> bids = bidMap.entrySet().stream()
                .map(e -> Map.of("price", e.getKey(), "quantity", e.getValue()))
                .toList();

        try {
            String snapshot = objectMapper.writeValueAsString(
                    Map.of("tokenId", tokenId, "asks", asks, "bids", bids)
            );
            template.convertAndSend(destination, snapshot);
        } catch (Exception e) {
            // 직렬화 실패 시 빈 호가창 전송
            template.convertAndSend(destination,
                    "{\"tokenId\":" + tokenId + ",\"asks\":[],\"bids\":[]}");
        }
    }
}

package server.main.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import server.main.global.util.MatchClient;

@Component
@RequiredArgsConstructor
public class OrderBookSubscribeHandler {

    private final SimpMessagingTemplate template;
    private final MatchClient matchClient;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        String destination = (String) event.getMessage().getHeaders()
                .get(SimpMessageHeaderAccessor.DESTINATION_HEADER);

        if (destination == null || !destination.startsWith("/topic/orderBook/")) {
            return;
        }

        Long tokenId = Long.parseLong(destination.replace("/topic/orderBook/", ""));

        try {
            String snapshot = matchClient.getOrderBookSnapshot(tokenId);
            template.convertAndSend(destination, snapshot);
        } catch (Exception e) {
            template.convertAndSend(destination,
                    "{\"tokenId\":" + tokenId + ",\"asks\":[],\"bids\":[]}");
        }
    }
}

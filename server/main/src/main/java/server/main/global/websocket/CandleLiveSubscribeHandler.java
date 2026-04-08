package server.main.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import server.main.candle.service.CandleLiveManager;
import server.main.candle.dto.LiveCandle;
import server.main.candle.entity.CandleType;
import server.main.candle.mapper.CandleMapper;

@Component
@RequiredArgsConstructor
public class CandleLiveSubscribeHandler {

    private final SimpMessagingTemplate template;
    private final CandleLiveManager candleLiveManager;
    private final CandleMapper candleMapper;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        String destination = (String) event.getMessage().getHeaders()
                .get(SimpMessageHeaderAccessor.DESTINATION_HEADER);

        // /topic/candle/live/{tokenId}
        if (destination == null || !destination.startsWith("/topic/candle/live/")) return;

        Long tokenId = Long.parseLong(destination.replace("/topic/candle/live/", ""));

        // 현재 봉 스냅샷 전부 전송
        for (CandleType type : CandleType.values()) {
            LiveCandle snapshot = candleLiveManager.getSnapshot(tokenId, type);
            if (snapshot != null) {
                template.convertAndSend(destination, candleMapper.toLiveDto(snapshot, type));
            }
        }
    }
}
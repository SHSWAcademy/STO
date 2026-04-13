package server.main.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import server.main.candle.service.CandleLiveManager;
import server.main.candle.dto.LiveCandleDto;
import server.main.candle.entity.CandleType;
import server.main.candle.mapper.CandleMapper;

@Component
@RequiredArgsConstructor
public class CandleLiveSubscribeHandler {
    // 상세 페이지 접속 시 현재 캔들 차트 스냅샷을 전달
    // 과거 데이터는 DB 에서 조회, 실시간 현재 데이터를 candleLiveManager 에서 가져온다

    private final SimpMessagingTemplate template;
    private final CandleLiveManager candleLiveManager;
    private final CandleMapper candleMapper;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        String destination = (String) event.getMessage().getHeaders()
                .get(SimpMessageHeaderAccessor.DESTINATION_HEADER);

        // /topic/candle/live/{tokenId}/{type}
        if (destination == null || !destination.startsWith("/topic/candle/live/")) return;

        // "/"을 기준으로 분기 처리
        String[] parts = destination.replace("/topic/candle/live/", "").split("/");
        if (parts.length != 2) return;

        Long tokenId = Long.parseLong(parts[0]);
        CandleType type = CandleType.valueOf(parts[1]); // 분, 시, 일, 월, 년 별로 분기

        LiveCandleDto snapshot = candleLiveManager.getSnapshot(tokenId, type);
        if (snapshot != null) {
            template.convertAndSend(destination, candleMapper.toLiveDto(snapshot, type));
        }
    }
}
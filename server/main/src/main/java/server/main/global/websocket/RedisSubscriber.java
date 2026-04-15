package server.main.global.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import server.main.candle.service.CandleLiveManager;
import server.main.log.tradeLog.entity.TradeLog;
import server.main.log.tradeLog.repository.TradeLogRepository;
import server.main.log.tradeLog.service.TradeLogService;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final TradeLogService tradeLogService;
    private final TokenRepository tokenRepository;
    private final SimpMessagingTemplate messagingTemplate; // 스프링이 메시지 브로커에게 메시지를 전달하도록 하는 템플릿(도구)
    private final CandleLiveManager candleLiveManager;
    private final ObjectMapper objectMapper;

    // action when message arrived 레디스가 메시지를 받을 때 동작하는 메서드
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel()); // byte -> String, Redis로 받을 채널
        String body = new String(message.getBody());       // byte -> String, Redis 에서 publish 받을 데이터 body

        // 레디스가 받은 메시지가 어떤 것인지 확인 (호가창, 거래 완료, 대기 주문)
        String[] parts = channel.split(":");
        String type = parts[0];

        if ("orderBook".equals(type)) {         // 호가창 메시지를 받았을 경우
            messagingTemplate.convertAndSend("/topic/orderBook/" + parts[1], body);
        } else if ("trades".equals(type)) {     // 거래 완료 메시지를 받았을 경우
            Long tokenId = Long.parseLong(parts[1]);
            messagingTemplate.convertAndSend("/topic/trades/" + parts[1], body);

            // 캔들 차트
            try {
                JsonNode node        = objectMapper.readTree(body);
                Long tradePrice      = node.get("tradePrice").asLong();
                Long tradeQuantity   = node.get("tradeQuantity").asLong();
                candleLiveManager.update(tokenId, tradePrice, tradeQuantity);

                // 주문 체결 로그 DB에 저장
                Token token = tokenRepository.findByIdWithAsset(tokenId).orElseThrow();
                String detail = String.format("토큰 이름=%s 가격=%.0f 금액=%.0f",
                        token.getAsset().getAssetName(), tradePrice, (tradePrice * tradeQuantity));
                tradeLogService.save(String.valueOf(tokenId), detail, true);
                // 현재 클래스에서 트랜잭션을 쓰지 않기 떄문에 requires new 옵션 필요 없음

            } catch (Exception e) {
                log.error("캔들 갱신 실패 - body: {}", body, e);
                tradeLogService.save(String.valueOf(tokenId), "체결 처리 실패: " + e.getMessage(), false);
            }

        } else if ("pendingOrders".equals(type)) { // 호가창 메시지를 받았을 경우
            messagingTemplate.convertAndSend("/topic/pendingOrders/" + parts[1] + "/" + parts[2], body);
        } else if ("alarm".equals(type)) {  // 알람창 메시지를 받았을 경우 alarm:{memberId} 으로 들어오면 /topic/alarm/{memberId} 로 convertAndSend
            messagingTemplate.convertAndSend("/topic/alarm/" + parts[1], body);
        }
    }
}

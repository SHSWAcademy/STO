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
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final TradeLogRepository tradeLogRepository;
    private final TokenRepository tokenRepository;
    private final SimpMessagingTemplate messagingTemplate; // 스프링이 메시지 브로커에게 메시지를 전달하도록 하는 템플릿(도구)
    private final CandleLiveManager candleLiveManager;
    private final ObjectMapper objectMapper;

    // action when message arrived 레디스가 메시지를 받을 때 동작하는 메서드
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel()); // byte -> String, Redis로 받을 채널
        String body = new String(message.getBody());       // byte -> String, Redis 에서 publish 받을 데이터 body

        // Redis checks if the message being published is related to the orderBook or trades or pendingOrders
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
                JsonNode node         = objectMapper.readTree(body);
                Double tradePrice     = node.get("tradePrice").asDouble();
                Double tradeQuantity  = node.get("tradeQuantity").asDouble();
                candleLiveManager.update(tokenId, tradePrice, tradeQuantity);

                // 주문 체결 로그 DB에 저장
                Token token = tokenRepository.findByIdWithAsset(tokenId).orElseThrow();
                String detail = String.format("건물 이름=%d 가격=%.0f 금액=%.0f",
                        token.getAsset().getAssetName(), (tradePrice * tradeQuantity), tradeQuantity);
                tradeLogRepository.save(TradeLog.builder()
                        .timeStamp(LocalDateTime.now())
                        .identifier(String.valueOf(tokenId)) // 토큰 id로 임의로 넣어뒀는데 이건 매치 작업 다 되면 받는 데이터로 member name 넣어둘게요 ..
                        .task("TRADE_EXECUTED")
                        .detail(detail)
                        .result(true)
                        .build());

            } catch (Exception e) {
                log.error("캔들 갱신 실패 - body: {}", body, e);
            }

        } else if ("pendingOrders".equals(type)) { // 호가창 메시지를 받았을 경우
            messagingTemplate.convertAndSend("/topic/pendingOrders/" + parts[1] + "/" + parts[2], body);
        }
    }
}

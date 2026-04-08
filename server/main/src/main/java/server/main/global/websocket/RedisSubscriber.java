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

@Component
@RequiredArgsConstructor
@Slf4j
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final CandleLiveManager candleLiveManager;
    private final ObjectMapper objectMapper;

    // action when message arrived 레디스가 메시지를 받을 때 동작하는 메서드
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        // Redis checks if the message being published is related to the orderBook or trades or pendingOrders
        // 레디스가 받은 메시지가 어떤 것인지 확인 (호가창, 거래 완료, 대기 주문)
        String[] parts = channel.split(":");
        String type = parts[0];

        // send to subscriber
        // 호가, 거래 완료 메시지는 매치 서버에서 받는다
        // 캔들 차트는 과거 봉 REST, 현재 봉은 trades 이벤트 기반으로 처리하므로 batch publish 없음 => 수정 : 배치 말고 메인에서 모두 봉 작업 필요
        if ("orderBook".equals(type)) {
            messagingTemplate.convertAndSend("/topic/orderBook/" + parts[1], body);
        } else if ("trades".equals(type)) {
            Long tokenId = Long.parseLong(parts[1]);
            messagingTemplate.convertAndSend("/topic/trades/" + parts[1], body);

            // 캔들 차트
            try {
                JsonNode node         = objectMapper.readTree(body);
                Double tradePrice     = node.get("tradePrice").asDouble();
                Double tradeQuantity  = node.get("tradeQuantity").asDouble();
                candleLiveManager.update(tokenId, tradePrice, tradeQuantity);
            } catch (Exception e) {
                log.error("캔들 갱신 실패 - body: {}", body, e);
            }

        } else if ("pendingOrders".equals(type)) { // 상세 페이지 - 대기 웹소켓
            messagingTemplate.convertAndSend("/topic/pendingOrders/" + parts[1] + "/" + parts[2], body);
        }
    }
}

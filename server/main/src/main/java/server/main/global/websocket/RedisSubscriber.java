package server.main.global.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisSubscriber implements MessageListener {

    private final SimpMessagingTemplate messagingTemplate;

    // 한글로 적으면 한 번씩 깨져서 영어랑 같이 적습니다 ㅠㅠ
    // action when message arrived 레디스가 메시지를 받을 때 동작하는 메서드
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        // Redis checks if the message being published is related to the orderBook or trades or candleChart
        // 레디스가 받은 메시지가 어떤 것인지 확인 (호가창, 거래 완료, 캔들 차트 관련)
        String[] parts = channel.split(":");
        String type = parts[0];

        // send to subscriber
        // 호가, 거래 완료 메시지는 매치 서버에서 받고, 캔들 차트 신호는 배치 서버로부터 받는다
        if ("orderBook".equals(type)) {
            messagingTemplate.convertAndSend("/topic/orderBook/" + parts[1], body);
        } else if ("trades".equals(type)) {
            messagingTemplate.convertAndSend("/topic/trades/" + parts[1], body);
        } else if ("candle".equals(type)) { // 상세 페이지 - 캔들 차트 마지막 봉 소켓으로 가져오기
            messagingTemplate.convertAndSend("/topic/candle/" + parts[1] + "/" + parts[2], body);
        } else if ("pendingOrders".equals(type)) { // 상세 페이지 - 대기 웹소켓
            messagingTemplate.convertAndSend("/topic/pendingOrders/" + parts[1] + "/" + parts[2], body);
        }
    }
}
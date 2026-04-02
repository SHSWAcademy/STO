package server.main.global.util;

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
    private final ObjectMapper objectMapper;

    // 한글로 적으면 한 번씩 깨져서 영어로 적습니다 ㅠㅠ
    // action when message arrived
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String channel = new String(message.getChannel());
        String body = new String(message.getBody());

        // Redis checks if the message being published is related to the orderBook or trades
        String[] parts = channel.split(":");
        String type = parts[0];     // check orderBook & trades
        String tokenId = parts[1];  // pk

        // send to subscriber
        if ("orderBook".equals(type)) {
            messagingTemplate.convertAndSend("/topic/orderBook/" + tokenId, body);
        } else if ("trades".equals(type)) {
            messagingTemplate.convertAndSend("/topic/trades/" + tokenId, body);
        }
    }
}
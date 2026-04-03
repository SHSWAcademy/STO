package server.main.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import server.main.global.websocket.RedisSubscriber;

@Configuration
public class RedisConfig {

    // Redis - use String Data Structure
    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        return template;
    }


    @Bean // register bean "messageListenerAdapter" and execute "onMessage" method by RedisSubscriber
    public MessageListenerAdapter messageListenerAdapter(RedisSubscriber redisSubscriber) {
        return new MessageListenerAdapter(redisSubscriber, "onMessage");
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory connectionFactory, MessageListenerAdapter messageListenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        // Subscribes to Redis topics matching the pattern "orderBook:*" to receive incoming data, 레디스에서 맞추어주는 라디오 주파수 같은 개념, 내가 이 토픽만 받을 것이다 ~ ..
        container.addMessageListener(messageListenerAdapter, new PatternTopic("orderBook:*"));
        // Subscribes to Redis topics matching the pattern "trades:*" to receive incoming data
        container.addMessageListener(messageListenerAdapter, new PatternTopic("trades:*"));
        return container;
    }
}

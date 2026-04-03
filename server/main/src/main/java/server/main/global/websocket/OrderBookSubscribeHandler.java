package server.main.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
@RequiredArgsConstructor
public class OrderBookSubscribeHandler {

    //- Stomp 에서는 순수 웹소켓과 다르게 직접 클라이언트 세션들을 개발자가 관리하지 않는다.
    //- 클라이언트 세션들은 Stomp 에서 자동으로 편리하게 관리하고 있다.
    //- 웹소켓을 이용하며 발생한 사건(event)들을 catch 하여 기록, 로깅할 수 있도록 돕는 객체가 EventListener 이다.
    //- 웹소켓 시스템의 기록 일지 역할

    private final SimpMessagingTemplate template;
    private final MatchClient matchClient;

    // 클라이언트가 상세 페이지 접속 시 stomp 실행 -> 그 때 발생하는 일을 이벤트 리스너가 확인
    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        // 클라이언트가 구독한 주소를 확인
        String destination = (String) event.getMessage().getHeaders()
                .get(SimpMessageHeaderAccessor.DESTINATION_HEADER);

        // 검증, 구독 : /topic 이 추가되도록 WebsocketConfig 에서 설정
        if (destination == null || !destination.startsWith("/topic/orderBook/")) return;

        String tokenId = destination.replace("/topic/orderBook/", "");

        // matchClient를 통해 match 서버에서 현재까지의 호가 정보 스냅샷을 받아 추출
        String snapshot = matchClient.getOrderBookSnapshot(Long.parseLong(tokenId));
        template.convertAndSend(destination, snapshot);
    }
}

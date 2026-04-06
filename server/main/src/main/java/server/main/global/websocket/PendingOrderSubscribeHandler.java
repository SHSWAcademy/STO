package server.main.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import server.main.global.security.JwtTokenProvider;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.entity.Order;
import server.main.order.mapper.OrderMapper;
import server.main.order.repository.OrderRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PendingOrderSubscribeHandler {

    private final SimpMessagingTemplate template;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;
    private final JwtTokenProvider jwtTokenProvider;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        String destination = (String) event.getMessage().getHeaders()
                .get(SimpMessageHeaderAccessor.DESTINATION_HEADER);

        if (destination == null || !destination.startsWith("/topic/pendingOrders/")) return;

        // JWT 검증 추가 (로그인 여부 확인)
        String token = (String) event.getMessage().getHeaders().get("Authorization");
        if (token == null || !token.startsWith("Bearer ")) return;
        token = token.substring(7);
        if (!jwtTokenProvider.validateToken(token)) return;

        // /topic/pendingOrders/{tokenId}/{memberId}
        String[] parts = destination.replace("/topic/pendingOrders/", "").split("/");
        if (parts.length < 2) return;

        // 주소 뒷 부분 파싱 -> tokenId, memberId 확인
        Long tokenId = Long.parseLong(parts[0]);
        Long memberId = Long.parseLong(parts[1]);

        // JWT의 실제 memberId와 URL의 memberId 비교 (본인 검증)
        Long actualMemberId = Long.parseLong(jwtTokenProvider.getClaims(token).getSubject());
        if (!actualMemberId.equals(memberId)) return;

        // DB 에서 미체결 주문 조회, snapshot 바로 전송
        // '대기' 탭을 누를 때 1회 발생
        List<Order> pendingOrders = orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId);
        List<PendingOrderResponseDto> dtos = orderMapper.toPendingDtoList(pendingOrders);

        template.convertAndSend(destination, dtos);
    }
}

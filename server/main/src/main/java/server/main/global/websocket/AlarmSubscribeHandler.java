package server.main.global.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import server.main.alarm.dto.AlarmResponseDto;
import server.main.alarm.repository.AlarmRepository;
import server.main.global.security.JwtTokenProvider;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AlarmSubscribeHandler {

    private final SimpMessagingTemplate template;
    private final AlarmRepository alarmRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        String destination = (String) event.getMessage().getHeaders()
                .get(SimpMessageHeaderAccessor.DESTINATION_HEADER);

        if (destination == null || !destination.startsWith("/topic/alarm/")) return;

        // JWT 검증
        SimpMessageHeaderAccessor accessor = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String token = accessor.getFirstNativeHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) return;
        token = token.substring(7);
        if (!jwtTokenProvider.validateToken(token)) return;

        // /topic/alarm/{memberId} 에서 memberId 파싱
        String pathMemberId = destination.replace("/topic/alarm/", "");
        Long memberId;
        try {
            memberId = Long.parseLong(pathMemberId);
        } catch (NumberFormatException e) {
            return;
        }

        // JWT의 실제 memberId와 URL의 memberId 비교 — 타인 알람 구독 차단
        Long actualMemberId = Long.parseLong(jwtTokenProvider.getClaims(token).getSubject());
        if (!actualMemberId.equals(memberId)) {
            log.warn("알람 구독 차단 — 요청 memberId: {}, JWT memberId: {}", memberId, actualMemberId);
            return;
        }

        // 미읽음 알람 스냅샷 즉시 전송
        List<AlarmResponseDto> unreadAlarms = alarmRepository
                .findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlarmResponseDto::from)
                .toList();

        template.convertAndSend(destination, unreadAlarms);
    }
}                                                         

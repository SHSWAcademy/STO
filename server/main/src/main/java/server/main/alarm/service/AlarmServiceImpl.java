package server.main.alarm.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.alarm.dto.AlarmResponseDto;
import server.main.alarm.entity.Alarm;
import server.main.alarm.entity.AlarmType;
import server.main.alarm.repository.AlarmRepository;
import server.main.global.error.BusinessException;

import java.util.List;

import static server.main.global.error.ErrorCode.ENTITY_NOT_FOUNT_ERROR;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlarmServiceImpl implements AlarmService {

    private final AlarmRepository alarmRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;


    // 알람 생성, Redis publish
    @Override
    @Transactional
    public void createAlarm(Long memberId, AlarmType type, Long tokenId, String alarmContent) {
        log.info("[Alarm] createAlarm 호출 - memberId: {}, type: {}, tokenId: {}", memberId, type, tokenId);
        Alarm alarm = Alarm.builder()
                .memberId(memberId)
                .alarmType(type)
                .tokenId(tokenId)
                .alarmContent(alarmContent)
                .build();

        alarmRepository.save(alarm); // 1. 저장

        // 2. 쏘기
        // Redis publish : /topic/alarm/{memberId}
        try {
            String payload = objectMapper.writeValueAsString(AlarmResponseDto.from(alarm));
            redisTemplate.convertAndSend("alarm:" + memberId, payload);
            log.info("[Alarm] Redis publish 성공 - channel: alarm:{}", memberId);
        } catch (Exception e) {
            log.error("[Alarm] Redis publish 실패 - memberId: {}", memberId, e);
        }
    }

    // 알람 리스트
    @Override
    public List<AlarmResponseDto> getAlarms(Long memberId) {
        return alarmRepository.findTop50ByMemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(AlarmResponseDto::from)
                .toList();
    }

    // 알림 단건 읽음 처리
    @Override
    @Transactional
    public void markAsRead(Long alarmId, Long memberId) {
        int updated = alarmRepository.markAsReadByAlarmIdAndMemberId(alarmId, memberId);
        if (updated == 0) {
            throw new BusinessException(ENTITY_NOT_FOUNT_ERROR);
        }
    }

    // 알림 전체 읽음 처리
    @Override
    @Transactional
    public void markAllAsRead(Long memberId) {
        alarmRepository.markAllAsRead(memberId);
    }
}

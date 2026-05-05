package server.main.alarm.service;

import server.main.alarm.dto.AlarmResponseDto;
import server.main.alarm.entity.AlarmType;

import java.util.List;

public interface AlarmService {
    // 알람 생성, Redis publish (트랜잭션 커밋 후 호출)
    void createAlarm(Long memberId, AlarmType type, Long tokenId, String alarmContent);

    // 알람 리스트
    List<AlarmResponseDto> getAlarms(Long memberId);

    // 알림 단건 읽음 처리
    void markAsRead(Long alarmId, Long memberId);

    // 알림 전체 읽음ㅁ 처리
    void markAllAsRead(Long memberId);

    // 미읽음 알림 수
    long getUnreadCount(Long memberId);
}

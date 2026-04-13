package server.main.alarm.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.alarm.entity.Alarm;

import java.util.List;
import java.util.Optional;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
    // 전체 목록 — 최신순 50개
    List<Alarm> findTop50ByMemberIdOrderByCreatedAtDesc(Long memberId);

    // 미읽음 목록 : isRead = false 알람 리스트
    List<Alarm> findByMemberIdAndIsReadFalseOrderByCreatedAtDesc(Long memberId);

    // 미읽음 수 : isRead = false 의 count
    long countByMemberIdAndIsReadFalse(Long memberId);

    // 단건 조회 (본인 확인용) : alarmId, memberId로 Alarm 조회
    Optional<Alarm> findByAlarmIdAndMemberId(Long alarmId, Long memberId);

    // 전체 읽음 (bulk update 처리)
    @Modifying
    @Query("UPDATE Alarm a SET a.isRead = true WHERE a.memberId = :memberId AND a.isRead = false")
    void markAllAsRead(@Param("memberId") Long memberId);
}

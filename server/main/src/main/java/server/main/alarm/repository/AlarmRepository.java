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

    // 단건 읽음 (bulk update 처리)
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Alarm a SET a.isRead = true WHERE a.alarmId = :alarmId AND a.memberId = :memberId AND a.isRead = false")
    int markAsReadByAlarmIdAndMemberId(@Param("alarmId") Long alarmId, @Param("memberId") Long memberId);

    // 전체 읽음 (bulk update 처리)
    // @Modifying : 조회 쿼리가 아닌 수정, 삭제 쿼리를 의미
    // JPA 영속성 컨텍스트 : @Modify 작용 시 1차 캐시를 거치지 않고 수정, 삭제 쿼리를 바로 DB에 flush
    // => DB는 수정, 삭제 쿼리가 반영된 데이터가 남아있지만 1차 캐시는 반영되지 않은 데이터가 남는다 => 캐시, DB 데이터 불일치
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Alarm a SET a.isRead = true WHERE a.memberId = :memberId AND a.isRead = false")
    void markAllAsRead(@Param("memberId") Long memberId);
}

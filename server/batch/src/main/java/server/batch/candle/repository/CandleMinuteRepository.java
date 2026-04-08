package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleMinute;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleMinuteRepository extends JpaRepository<CandleMinute, Long> {

    // 1시간 동안 존재하는 tokenId 목록
    // SELECT DISTINCT token_id FROM candle_minutes
    // WHERE candle_time BETWEEN '2026-04-07 14:00:00' AND '2026-04-07 15:00:00';
    List<Long> findDistinctTokenIdByCandleTimeBetween(LocalDateTime from, LocalDateTime to);

    List<CandleMinute> findByTokenIdAndCandleTimeBetween(Long tokenId, LocalDateTime from, LocalDateTime to);
}

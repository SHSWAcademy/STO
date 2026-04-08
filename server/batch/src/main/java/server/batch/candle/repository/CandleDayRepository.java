package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleDay;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleDayRepository extends JpaRepository<CandleDay, Long> {

    // 이번 달 동안 존재하는 tokenId 목록
    List<Long> findDistinctTokenIdByCandleTimeBetween(LocalDateTime from, LocalDateTime to);

    List<CandleDay> findByTokenIdAndCandleTimeBetween(Long tokenId, LocalDateTime from, LocalDateTime to);
}

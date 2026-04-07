package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleMonth;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleMonthRepository extends JpaRepository<CandleMonth, Long> {

    // 올해 동안 존재하는 tokenId 목록
    List<Long> findDistinctTokenIdByCandleTimeBetween(LocalDateTime from, LocalDateTime to);

    List<CandleMonth> findByTokenIdAndCandleTimeBetween(Long tokenId, LocalDateTime from, LocalDateTime to);
}

package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleHour;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleHourRepository extends JpaRepository<CandleHour, Long> {

    List<Long> findDistinctTokenIdByCandleTimeBetween(LocalDateTime from, LocalDateTime to);

    List<CandleHour> findByTokenIdAndCandleTimeBetween(Long tokenId, LocalDateTime from, LocalDateTime to);
}

package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleHour;

public interface CandleHourRepository extends JpaRepository<CandleHour, Long> {
}

package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleDay;

public interface CandleDayRepository extends JpaRepository<CandleDay, Long> {
}

package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleYear;

public interface CandleYearRepository extends JpaRepository<CandleYear, Long> {
}

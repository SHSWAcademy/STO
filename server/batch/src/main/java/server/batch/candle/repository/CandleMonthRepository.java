package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleMonth;

public interface CandleMonthRepository extends JpaRepository<CandleMonth, Long> {
}

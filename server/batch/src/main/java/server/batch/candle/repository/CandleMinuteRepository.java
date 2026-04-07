package server.batch.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.candle.entity.CandleMinute;

public interface CandleMinuteRepository extends JpaRepository<CandleMinute, Long> {
}

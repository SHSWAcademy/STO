package server.main.log.tradeLog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.log.tradeLog.entity.TradeLog;

public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
}

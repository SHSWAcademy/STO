package server.batch.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.trade.entity.Trade;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByTokenIdAndExecutedAtBetween(Long tokenId, LocalDateTime from, LocalDateTime to);
}

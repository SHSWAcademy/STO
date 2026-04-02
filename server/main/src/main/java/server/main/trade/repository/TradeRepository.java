package server.main.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.trade.entity.Trade;

public interface TradeRepository extends JpaRepository<Trade, Long> {

}

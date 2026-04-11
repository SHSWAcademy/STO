package server.main.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.trade.entity.TradeDuplicated;

public interface TradeDuplicatedRepository extends JpaRepository<TradeDuplicated, Long> {
}

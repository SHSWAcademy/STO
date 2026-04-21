package server.batch.trade.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import server.batch.trade.entity.Trade;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    List<Trade> findByTokenIdAndExecutedAtBetween(Long tokenId, LocalDateTime from, LocalDateTime to);

    @Modifying
    @Transactional
    @Query(value = "UPDATE trades t SET settlement_status = :status WHERE trade_Id = :tradeId", nativeQuery = true)
    void updateSettlementStatus(@Param("tradeId") Long tradeId, @Param("status") String status);
}

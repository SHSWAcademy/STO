package server.main.trade.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.trade.entity.Trade;

import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    @Query("SELECT t FROM Trade t WHERE t.token.tokenId = :tokenId ORDER BY t.createdAt DESC LIMIT 50")
    List<Trade> findTradeList(Long tokenId);

    @Query("SELECT COALESCE(SUM(t.tradeQuantity), 0) FROM Trade t WHERE t.token.tokenId = :tokenId AND t.createdAt >= CURRENT_DATE AND t.createdAt < CURRENT_DATE + 1 DAY")
    Long sumDailyVolume(Long tokenId);

    @Query("SELECT t.token.tokenId, SUM(t.totalTradePrice), SUM(t.tradeQuantity) FROM Trade t WHERE t.token.tokenId IN :tokenIds GROUP BY t.token.tokenId")
    List<Object[]> findAggregatesByTokenIds(@Param("tokenIds") List<Long> tokenIds);
}

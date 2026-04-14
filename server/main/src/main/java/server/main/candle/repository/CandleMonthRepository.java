package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMinute;
import server.main.candle.entity.CandleMonth;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleMonthRepository extends JpaRepository<CandleMonth, Long> {

    @Query("SELECT c FROM CandleMonth c WHERE c.token.tokenId = :tokenId ORDER BY c.candleTime DESC LIMIT 1")
    Optional<CandleMonth> findLatest(@Param("tokenId") Long tokenId);

    @Query("SELECT c FROM CandleMonth c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleMonth> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);

    @Query("SELECT c FROM CandleMonth c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :startOfMonth AND c.candleTime < :endOfMonth")
    List<CandleMonth> findThisMonthByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth);

    @Query("SELECT c FROM CandleMonth c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :since ORDER BY c.token.tokenId ASC, c.candleTime ASC")
    List<CandleMonth> findRecentByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("since") LocalDateTime since);

    @Modifying
    @Query(nativeQuery = true, value =
        "INSERT INTO candle_months (token_id, candle_time, open_price, high_price, low_price, close_price, volume, trade_count) " +
        "VALUES (:tokenId, :candleTime, :openPrice, :highPrice, :lowPrice, :closePrice, :volume, :tradeCount) " +
        "ON CONFLICT (token_id, candle_time) DO UPDATE SET " +
        "high_price = EXCLUDED.high_price, low_price = EXCLUDED.low_price, " +
        "close_price = EXCLUDED.close_price, volume = EXCLUDED.volume, trade_count = EXCLUDED.trade_count")
    void upsert(@Param("tokenId") Long tokenId, @Param("candleTime") LocalDateTime candleTime,
                @Param("openPrice") Long openPrice, @Param("highPrice") Long highPrice,
                @Param("lowPrice") Long lowPrice, @Param("closePrice") Long closePrice,
                @Param("volume") Long volume, @Param("tradeCount") Integer tradeCount);
}

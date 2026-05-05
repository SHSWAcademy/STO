package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMinute;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleMinuteRepository extends JpaRepository<CandleMinute, Long> {

    @Query("SELECT c FROM CandleMinute c WHERE c.token.tokenId = :tokenId ORDER BY c.candleTime DESC LIMIT 1")
    Optional<CandleMinute> findLatest(@Param("tokenId") Long tokenId);

    // candleTime 인덱스 생성해두기
    @Query("SELECT c FROM CandleMinute c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleMinute> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);

    // 서버 재시작 후 같은 구간 재저장 시 중복 방지 — 이미 있으면 OHLCV 갱신
    @Modifying
    @Query(nativeQuery = true, value =
        "INSERT INTO candle_minutes (token_id, candle_time, open_price, high_price, low_price, close_price, volume, trade_count) " +
        "VALUES (:tokenId, :candleTime, :openPrice, :highPrice, :lowPrice, :closePrice, :volume, :tradeCount) " +
        "ON CONFLICT (token_id, candle_time) DO UPDATE SET " +
        "high_price = EXCLUDED.high_price, low_price = EXCLUDED.low_price, " +
        "close_price = EXCLUDED.close_price, volume = EXCLUDED.volume, trade_count = EXCLUDED.trade_count")
    void upsert(@Param("tokenId") Long tokenId, @Param("candleTime") LocalDateTime candleTime,
                @Param("openPrice") Long openPrice, @Param("highPrice") Long highPrice,
                @Param("lowPrice") Long lowPrice, @Param("closePrice") Long closePrice,
                @Param("volume") Long volume, @Param("tradeCount") Integer tradeCount);
}

package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMinute;

import java.time.LocalDateTime;

public interface CandleMinuteRepository extends JpaRepository<CandleMinute, Long> {

    // 특정 토큰의 기간 내 1분봉 최고가/최저가 집계
    @Query("SELECT MAX(c.highPrice) FROM CandleMinute c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findMinuteHighPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);

    @Query("SELECT MIN(c.lowPrice) FROM CandleMinute c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findMinuteLowPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);
}

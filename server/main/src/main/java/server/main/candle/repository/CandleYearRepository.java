package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleYear;

import java.time.LocalDateTime;

public interface CandleYearRepository extends JpaRepository<CandleYear, Long> {

    // 특정 토큰의 기간 내 연봉 최고가/최저가 집계
    @Query("SELECT MAX(c.highPrice) FROM CandleYear c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findYearHighPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);

    @Query("SELECT MIN(c.lowPrice) FROM CandleYear c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findYearLowPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);
}

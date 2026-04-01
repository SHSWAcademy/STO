package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMonth;

import java.time.LocalDateTime;

public interface CandleMonthRepository extends JpaRepository<CandleMonth, Long> {

    // 특정 토큰의 기간 내 월봉 최고가/최저가 집계
    @Query("SELECT MAX(c.highPrice) FROM CandleMonth c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findMonthHighPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);

    @Query("SELECT MIN(c.lowPrice) FROM CandleMonth c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findMonthLowPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);
}

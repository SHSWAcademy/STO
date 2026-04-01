package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleHour;

import java.time.LocalDateTime;

public interface CandleHourRepository extends JpaRepository<CandleHour, Long> {

    // 특정 토큰의 기간 내 1시간봉 최고가/최저가 집계
    @Query("SELECT MAX(c.highPrice) FROM CandleHour c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findHourHighPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);

    @Query("SELECT MIN(c.lowPrice) FROM CandleHour c WHERE c.token.tokenId = :tokenId AND c.candleTime >= :from")
    Double findHourLowPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);
}

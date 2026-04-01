package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleDay;

import java.time.LocalDateTime;

public interface CandleDayRepository extends JpaRepository<CandleDay, Long> {

    // 특정 토큰의 기간 내 일봉 최고가/최저가 집계
    @Query("SELECT MAX(c.highPrice) FROM CandleDay c WHERE c.token.tokenId =:tokenId AND c.candleTime >= :from")
    Double findDayHighPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);

    @Query("SELECT MIN(c.lowPrice) FROM CandleDay c WHERE c.token.tokenId =:tokenId AND c.candleTime >= :from")
    Double findDayLowPrice(@Param("tokenId") Long tokenId, @Param("from") LocalDateTime from);
}

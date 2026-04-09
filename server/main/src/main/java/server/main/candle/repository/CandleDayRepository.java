package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleDay;
import server.main.candle.entity.CandleMinute;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleDayRepository extends JpaRepository<CandleDay, Long> {
    @Query("SELECT c FROM CandleDay c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleDay> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);

    @Query("SELECT c FROM CandleDay c WHERE c.token.tokenId IN :tokenIds AND DATE(c.candleTime) = CURRENT_DATE")
    List<CandleDay> findTodayByTokenIds(@Param("tokenIds") List<Long> tokenIds);
}

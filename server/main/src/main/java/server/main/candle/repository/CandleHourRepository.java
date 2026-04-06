package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleDay;
import server.main.candle.entity.CandleHour;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleHourRepository extends JpaRepository<CandleHour, Long> {
    @Query("SELECT c FROM CandleHour c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleHour> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);
}

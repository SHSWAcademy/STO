package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleDay;
import server.main.candle.entity.CandleMinute;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleDayRepository extends JpaRepository<CandleDay, Long> {

    @Query("SELECT c FROM CandleDay c WHERE c.token.tokenId = :tokenId ORDER BY c.candleTime DESC LIMIT 1")
    Optional<CandleDay> findLatest(@Param("tokenId") Long tokenId);

    @Query("SELECT c FROM CandleDay c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleDay> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);

    @Query("SELECT c FROM CandleDay c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :startOfDay AND c.candleTime < :endOfDay")
    List<CandleDay> findTodayByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT c FROM CandleDay c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :since ORDER BY c.token.tokenId ASC, c.candleTime ASC")
    List<CandleDay> findRecentByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("since") LocalDateTime since);
}

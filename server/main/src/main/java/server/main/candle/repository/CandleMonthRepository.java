package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMinute;
import server.main.candle.entity.CandleMonth;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleMonthRepository extends JpaRepository<CandleMonth, Long> {
    @Query("SELECT c FROM CandleMonth c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleMonth> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);

    @Query("SELECT c FROM CandleMonth c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :startOfMonth AND c.candleTime < :endOfMonth")
    List<CandleMonth> findThisMonthByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("startOfMonth") LocalDateTime startOfMonth, @Param("endOfMonth") LocalDateTime endOfMonth);

    @Query("SELECT c FROM CandleMonth c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :since ORDER BY c.token.tokenId ASC, c.candleTime ASC")
    List<CandleMonth> findRecentByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("since") LocalDateTime since);
}

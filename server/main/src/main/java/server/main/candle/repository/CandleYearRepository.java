package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMonth;
import server.main.candle.entity.CandleYear;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CandleYearRepository extends JpaRepository<CandleYear, Long> {

    @Query("SELECT c FROM CandleYear c WHERE c.token.tokenId = :tokenId ORDER BY c.candleTime DESC LIMIT 1")
    Optional<CandleYear> findLatest(@Param("tokenId") Long tokenId);

    @Query("SELECT c FROM CandleYear c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleYear> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);

    @Query("SELECT c FROM CandleYear c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :startOfYear AND c.candleTime < :endOfYear")
    List<CandleYear> findThisYearByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("startOfYear") LocalDateTime startOfYear, @Param("endOfYear") LocalDateTime endOfYear);

    @Query("SELECT c FROM CandleYear c WHERE c.token.tokenId IN :tokenIds AND c.candleTime >= :since ORDER BY c.token.tokenId ASC, c.candleTime ASC")
    List<CandleYear> findRecentByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("since") LocalDateTime since);
}

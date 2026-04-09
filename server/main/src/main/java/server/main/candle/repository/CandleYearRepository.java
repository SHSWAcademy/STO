package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMonth;
import server.main.candle.entity.CandleYear;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleYearRepository extends JpaRepository<CandleYear, Long> {
    @Query("SELECT c FROM CandleYear c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleYear> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);

    @Query("SELECT c FROM CandleYear c WHERE c.token.tokenId IN :tokenIds AND YEAR(c.candleTime) = YEAR(CURRENT_DATE)")
    List<CandleYear> findThisYearByTokenIds(@Param("tokenIds") List<Long> tokenIds);
}

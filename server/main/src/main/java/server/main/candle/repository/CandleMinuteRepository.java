package server.main.candle.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.candle.entity.CandleMinute;

import java.time.LocalDateTime;
import java.util.List;

public interface CandleMinuteRepository extends JpaRepository<CandleMinute, Long> {
    // candleTime 인덱스 생성해두기
    @Query("SELECT c FROM CandleMinute c WHERE c.token.tokenId = :tokenId AND c.candleTime < :before ORDER BY c.candleTime DESC LIMIT 35")
    List<CandleMinute> findTop35Before(@Param("tokenId") Long tokenId, @Param("before") LocalDateTime before);
}

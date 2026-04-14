package server.main.trade.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.trade.entity.Trade;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    @Query("SELECT t FROM Trade t WHERE t.token.tokenId = :tokenId ORDER BY t.createdAt DESC LIMIT 50")
    List<Trade> findTradeList(Long tokenId);

    @Query("SELECT COALESCE(SUM(t.tradeQuantity), 0) FROM Trade t WHERE t.token.tokenId = :tokenId AND t.createdAt >= CURRENT_DATE AND t.createdAt < CURRENT_DATE + 1 DAY")
    Long sumDailyVolume(Long tokenId);

    @Query("SELECT t.token.tokenId, SUM(t.totalTradePrice), SUM(t.tradeQuantity) FROM Trade t WHERE t.token.tokenId IN :tokenIds GROUP BY t.token.tokenId")
    List<Object[]> findAggregatesByTokenIds(@Param("tokenIds") List<Long> tokenIds);

    // 구매유저의 통 추자 금액 조회 (admin)
    @Query("SELECT t.buyer.memberId , SUM(t.totalTradePrice) FROM Trade t "
    + "WHERE t.buyer.memberId IN :memberIds GROUP BY t.buyer.memberId")
    List<Object[]> sumTotalBuyerUser(@Param("memberIds") List<Long> memberIds);

    // 대시보드 거래내역 조회
    @Query(value = "SELECT t FROM Trade t JOIN FETCH t.seller JOIN FETCH t.buyer JOIN FETCH t.token",
           countQuery = "SELECT COUNT(t) FROM Trade t")
    Page<Trade> findAllWithDetails(Pageable pageable);

    // 일일, 누적 체결수 / 일일, 누적 체결금액 조회
    @Query(value = """                                                                                                                                               
      SELECT
          COUNT(CASE WHEN created_at >= :start AND created_at < :end THEN 1 END),                                                                                  
          COUNT(*),
          COALESCE(SUM(CASE WHEN created_at >= :start AND created_at < :end THEN total_trade_price END), 0),
          COALESCE(SUM(total_trade_price), 0)
      FROM trades
      """, nativeQuery = true)
    Object[] findTradeStats(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

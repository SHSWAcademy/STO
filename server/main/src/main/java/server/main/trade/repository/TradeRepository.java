package server.main.trade.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.myAccount.dto.SellHistoryResponse;
import server.main.trade.entity.Trade;

import java.time.LocalDateTime;
import java.util.List;

public interface TradeRepository extends JpaRepository<Trade, Long> {
    @Query("SELECT COALESCE(SUM(t.totalTradePrice), 0) FROM Trade t WHERE t.executedAt >= :since")
    Long sumAllTodayTradeValue(@Param("since") LocalDateTime since);

    @Query("SELECT t FROM Trade t WHERE t.token.tokenId = :tokenId AND t.executedAt >= :since ORDER BY t.executedAt DESC")
    List<Trade> findTradeList(@Param("tokenId") Long tokenId, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.tradeQuantity), 0) FROM Trade t WHERE t.token.tokenId = :tokenId AND t.executedAt >= :since")
    Long sumDailyVolume(@Param("tokenId") Long tokenId, @Param("since") LocalDateTime since);

    @Query("SELECT t.token.tokenId, SUM(t.totalTradePrice), SUM(t.tradeQuantity) FROM Trade t WHERE t.token.tokenId IN :tokenIds AND t.executedAt >= :since GROUP BY t.token.tokenId")
    List<Object[]> findAggregatesByTokenIds(@Param("tokenIds") List<Long> tokenIds, @Param("since") LocalDateTime since);

    @Query("SELECT COALESCE(SUM(t.totalTradePrice), 0) FROM Trade t WHERE t.token.tokenId = :tokenId AND t.executedAt >= :since")
    Long sumDailyTradeValue(@Param("tokenId") Long tokenId, @Param("since") LocalDateTime since);

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

    @Query("""
        SELECT
            FUNCTION('DATE', t.executedAt),
            SUM(t.tradeQuantity),
            SUM(t.totalTradePrice),
            AVG(t.tradePrice)
        FROM Trade t
        WHERE t.token.tokenId = :tokenId
          AND t.executedAt >= :from
        GROUP BY FUNCTION('DATE', t.executedAt), t.settlementStatus
        ORDER BY FUNCTION('DATE', t.executedAt) ASC
    """)
    List<Object[]> findWeeklyTradeStats(
            @Param("tokenId") Long tokenId,
            @Param("from") LocalDateTime from
    );

    // 거래 테이블에서 오프, 온체인별 현황조회
    @Query("""
        SELECT COUNT(t),
               SUM(CASE WHEN t.settlementStatus = 'ON_CHAIN_PENDING' THEN 1 ELSE 0 END),
               SUM(CASE WHEN t.settlementStatus = 'SUCCESS' THEN 1 ELSE 0 END),
               COALESCE(SUM(t.totalTradePrice), 0)
        FROM Trade t
    """)
    List<Object[]> findGlobalSettlementStats();

    // 토큰별 오프체인 현황 조회 (admin)
    @Query("""
        SELECT t.token.tokenId,
               t.token.tokenSymbol,
               COUNT(t),
               SUM(CASE WHEN t.settlementStatus = 'ON_CHAIN_PENDING' THEN 1 ELSE 0 END),
               COALESCE(SUM(t.totalTradePrice), 0),
               t.token.contractAddress
        FROM Trade t
        GROUP BY t.token.tokenId, t.token.tokenSymbol, t.token.contractAddress
    """)
    List<Object[]> findTokenSettlementStats();

    @Query("""
      SELECT new server.main.myAccount.dto.SellHistoryResponse(
          t.tradeId,
          t.token.tokenName,
          t.token.tokenSymbol,
          t.tradeQuantity,
          t.totalTradePrice,
          t.feeAmount,
          t.executedAt
      )
      FROM Trade t
      WHERE t.seller.memberId = :memberId
        AND t.executedAt >= :start
        AND t.executedAt < :end
      ORDER BY t.executedAt DESC
  """)
    Page<SellHistoryResponse> findSellHistoryByMemberId(
            @Param("memberId") Long memberId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable
    );

    @Query("""
        SELECT COALESCE(SUM(t.totalTradePrice), 0),
               COALESCE(SUM(t.feeAmount), 0),
               COALESCE(SUM(t.tradeQuantity), 0)
        FROM Trade t
        WHERE t.buyOrder.orderId = :orderId
           OR t.sellOrder.orderId = :orderId
    """)
    Object[] findExecutionSummaryByOrderId(@Param("orderId") Long orderId);
}

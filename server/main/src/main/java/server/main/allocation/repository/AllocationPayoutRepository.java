package server.main.allocation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.allocation.entity.AllocationPayout;
import server.main.myaccount.dto.DividendHistoryResponse;

public interface AllocationPayoutRepository extends JpaRepository<AllocationPayout, Long> {
    // 회원 + 연도 필터 (AllocationEvent JOIN)
    @Query("""
      SELECT new server.main.myaccount.dto.DividendHistoryResponse(
          p.allocationPayoutId,
          t.tokenName,
          t.tokenSymbol,
          p.holdingQuantity,
          p.memberIncome / p.holdingQuantity,
          p.memberIncome,
          e.settlementYear,
          e.settlementMonth,
          p.createdAt
      )
      FROM AllocationPayout p
      JOIN AllocationEvent e ON p.allocationEventId = e.allocationEventId
      JOIN Token t ON t.tokenId = p.tokenId
      WHERE p.memberId = :memberId
         AND e.settlementYear = :year
         AND p.allocationPayoutStatus = 'SUCCESS'
      ORDER BY p.createdAt DESC
  """)
    Page<DividendHistoryResponse> findDividendHistoryByMemberIdAndYear(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            Pageable pageable
    );

    @Query("""
    SELECT new server.main.myaccount.dto.DividendHistoryResponse(
        p.allocationPayoutId,
        t.tokenName,
        t.tokenSymbol,
        p.holdingQuantity,
        p.memberIncome / p.holdingQuantity,
        p.memberIncome,
        e.settlementYear,
        e.settlementMonth,
        p.createdAt
    )
    FROM AllocationPayout p
    JOIN AllocationEvent e ON p.allocationEventId = e.allocationEventId
    JOIN Token t ON t.tokenId = p.tokenId
    WHERE p.memberId = :memberId
       AND e.settlementYear = :year
       AND e.settlementMonth = :month 
       AND p.allocationPayoutStatus = 'SUCCESS'
    ORDER BY p.createdAt DESC
  """)
    Page<DividendHistoryResponse> findDividendHistoryByMemberIdAndYearAndMonth(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month,
            Pageable pageable
    );
    // 이번달 배당금 합산 (AllocationEvent JOIN)
    @Query("""
      SELECT COALESCE(SUM(p.memberIncome), 0)
      FROM AllocationPayout p
      JOIN AllocationEvent e ON p.allocationEventId = e.allocationEventId
      WHERE p.memberId = :memberId
        AND e.settlementYear = :year
        AND e.settlementMonth = :month
        AND p.allocationPayoutStatus = 'SUCCESS'
  """)
    Long sumByMemberIdAndYearMonth(
            @Param("memberId") Long memberId,
            @Param("year") int year,
            @Param("month") int month
    );

    // 연도 전체 배당금 합산
    @Query("""
      SELECT COALESCE(SUM(p.memberIncome), 0)
      FROM AllocationPayout p
      JOIN AllocationEvent e ON p.allocationEventId = e.allocationEventId
      WHERE p.memberId = :memberId
        AND e.settlementYear = :year
         AND p.allocationPayoutStatus = 'SUCCESS'
  """)
    Long sumByMemberIdAndYear(
            @Param("memberId") Long memberId,
            @Param("year") int year
    );
}

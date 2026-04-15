package server.main.allocation.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.allocation.entity.AllocationPayout;

public interface AllocationPayoutRepository extends JpaRepository<AllocationPayout, Long> {
    // 회원 + 연도 필터 (AllocationEvent JOIN)
    @Query("""
      SELECT p FROM AllocationPayout p
      JOIN AllocationEvent e ON p.allocationEventId = e.allocationEventId
      WHERE p.memberId = :memberId AND e.settlementYear = :year
      ORDER BY e.settledAt DESC
  """)
    Page<AllocationPayout> findByMemberIdAndYear(
            @Param("memberId") Long memberId,
            @Param("year") int year,
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
  """)
    Long sumByMemberIdAndYear(
            @Param("memberId") Long memberId,
            @Param("year") int year
    );
}

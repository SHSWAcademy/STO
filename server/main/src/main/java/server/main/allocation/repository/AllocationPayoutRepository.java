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
}

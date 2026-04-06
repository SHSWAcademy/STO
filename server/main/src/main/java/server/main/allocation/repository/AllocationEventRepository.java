package server.main.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.main.allocation.entity.AllocationEvent;

import java.util.List;

public interface AllocationEventRepository extends JpaRepository<AllocationEvent, Long> {
    // 현재월 기준으로 자산 배당 리스트 조회
    @Query("SELECT a FROM AllocationEvent a WHERE YEAR(a.createdAt) = YEAR(CURRENT_DATE) AND MONTH(a.createdAt) = MONTH(CURRENT_DATE )")
    List<AllocationEvent> findAllCurrentMonthList();
}

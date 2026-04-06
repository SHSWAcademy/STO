package server.main.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.allocation.entity.AllocationEvent;

import java.util.List;

public interface AllocationEventRepository extends JpaRepository<AllocationEvent, Long> {
    // 현재월 기준으로 자산 배당 리스트 조회
    @Query("SELECT a FROM AllocationEvent a WHERE YEAR(a.createdAt) = :year AND MONTH(a.createdAt) = :month")
    List<AllocationEvent> findAllBySettlementMonth(@Param("year") int year, @Param("month") int month);

    // 현재월에 배당 스케줄 등록했는지 검증
    boolean existsByAssetIdAndSettlementYearAndSettlementMonth(
            Long assetId, int settlementYear, int settlementMonth);

    // 자산별 배당 스케줄내역 상세조회
    List<AllocationEvent> findAllocationEventsByAssetIdOrderByCreatedAt(Long assetId);
    // List<AllocationEvent> findAllByAssetId(Long assetId);

    List<AllocationEvent> findAllByAssetIdOrderBySettledAtDesc(Long assetId);
}

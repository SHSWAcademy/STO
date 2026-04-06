package server.main.allocation.entity;

import jakarta.persistence.*;
import lombok.*;
import server.main.global.util.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
@Table(name = "allocation_events")
public class AllocationEvent extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long allocationEventId;     // 배당관리ID
    @Column(name = "asset_id", insertable = false, updatable = false)
    private Long assetId;               // 자산 ID
    private Boolean allocationBatchStatus;  // 배치 여부
    private Long monthlyDividendIncome;   // 월 수익
    private LocalDateTime settledAt;
}

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
    private Long assetId;               // 자산ID
    private Boolean allocationBatchStatus;  // 배치여부
    private Long monthlyDividendIncome;   // 월 수익
    private LocalDateTime settledAt;     // 배당 지급일
    private int settlementYear;           // 배당 지급연도
    private int settlementMonth;          // 배당 지급월
    private Long disclosureId;            // 공시ID (파일 참조)

    // 배치 이벤트 수정용
    public void updateAllocationEvent(Long monthlyDividendIncome) {
        this.monthlyDividendIncome = monthlyDividendIncome;
    }
}

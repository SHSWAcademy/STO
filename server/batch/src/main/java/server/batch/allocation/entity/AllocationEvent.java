package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@ToString
@Table(name = "allocation_events")
public class AllocationEvent {

    @Id
    private Long allocationEventId; // 배당 관리ID

    private Long assetId;           // 자산ID
    private Long disclosureId;      // 공시ID
    private Boolean allocationBatchStatus; // 배치 실행여부
    private Long monthlyDividendIncome;    // 월수익
    private LocalDateTime settledAt;       // 배당 지급일
    private Integer settlementYear;        // 배당년도
    private Integer settlementMonth;       // 배당월
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 배당 성공 시
    public void complete() {
        this.allocationBatchStatus = true;
        this.settledAt = LocalDateTime.now();
    }
}

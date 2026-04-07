package server.main.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AllocationUpdateRequestDTO {
    private Long monthlyDividendIncome;    // 월 수익
    private Boolean allocationBatchStatus;  // 지급여부
    private Long disclosureId;              // 공시ID (파일수정용)
}

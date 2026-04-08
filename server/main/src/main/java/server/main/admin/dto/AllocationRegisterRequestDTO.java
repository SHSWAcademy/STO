package server.main.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AllocationRegisterRequestDTO {
    private Long assetId;       // 부동산ID
    private Long monthlyDividendIncome;   // 월 수익

}

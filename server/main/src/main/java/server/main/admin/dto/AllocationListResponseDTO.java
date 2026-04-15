package server.main.admin.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.YearMonth;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AllocationListResponseDTO {
    private Long assetId;       // 자산ID
    private String assetName;   // 자산이름
    private String imgUrl;      // 자산이미지
    private String tokenSymbol; // 토큰 심볼
    private Long monthlyDividendIncome;   // 월 수익
    private Boolean allocationBatchStatus; // 배당 지급 여부
    private YearMonth targetMonth;      // 배당 정산월
    private LocalDate allocateSetMonth; // 관리자 마감월
    private Long remainder;     // 배당 잔여금 (전월)
}

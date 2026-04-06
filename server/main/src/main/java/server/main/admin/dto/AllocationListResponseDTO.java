package server.main.admin.dto;

import lombok.*;

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

}

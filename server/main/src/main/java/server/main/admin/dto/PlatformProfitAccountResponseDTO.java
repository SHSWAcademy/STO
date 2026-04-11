package server.main.admin.dto;

import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PlatformProfitAccountResponseDTO {

    private Long platformCommRevenue;       // 플랫폼 누적수수료 수익
    private Long platformAllocationTotalAmount; // 플랫폼 누적 배당 수익
    private Long PlatformAssetValue;        // 플랫폼 보유 자산 가치 (초기 금액 기준)
    private Long PlatformAssetValueCurrent; // 플랫폼 보유 자산 가치 (토큰 현재가)

    List<PlatformTokenHoldingsDetailDTO> platformTokenHoldingsDetailList;  // 플랫폼 보유 토큰 상세 내역
    List<PlatformBankingListDTO> platformBankingList;   // 일자별 수수료 수익 추이 계산용

}

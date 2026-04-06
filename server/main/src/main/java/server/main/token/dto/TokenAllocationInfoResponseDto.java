package server.main.token.dto;

import lombok.*;
import server.main.allocation.entity.AllocationPayoutStatus;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenAllocationInfoResponseDto {
    // 상세 페이지 -> 배당금 내역
    // 필요 테이블 : ALLOCATION_EVENTS, ALLOCATION_PAYOUTS, TOKENS
    private LocalDateTime settledAt;    // 배당 지급일 (ALLOCATION_EVENTS 테이블)
    private Long allocationPerToken;     // 주당 배당금 (토큰 a의 배당 월 수익 / 토큰 전체 수) -> ALLOCATION_EVENT, TOKENS 테이블, 별도 컬럼 필요 ?
    private Long monthlyDividendIncome; // 총 배당금 (ALLOCATION_EVENTS 테이블)
    private Boolean allocationBatchStatus;  // 배치여부

}

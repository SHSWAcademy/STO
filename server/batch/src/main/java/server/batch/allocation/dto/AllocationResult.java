package server.batch.allocation.dto;

import lombok.Builder;
import lombok.Getter;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.entity.AssetAccount;

import java.util.List;

@Getter
@Builder
public class AllocationResult {

    private AllocationEvent event;          // 배당 이벤트 (assetId, monthlyDividendIncome 등)

    private AssetAccount assetAccount;      // 자산 계좌 (Writer에서 remainder 반환 시 사용)
    private long totalDeduction;            // 총 배당 지급액 (monthlyDividendIncome 전체)

    private List<MemberPayoutResult> memberPayouts; // 회원별 배당 결과 목록

    private long platformAmount;            // 플랫폼 배당금
    private Long tokenId;                   // 토큰 ID (assetId → tokenId 변환 결과)
    private String tokenName;              // 토큰명 (알람 메시지용)
    private long remainder;                 // 버림으로 생긴 잔액 (Writer에서 자산 계좌로 반환)
}

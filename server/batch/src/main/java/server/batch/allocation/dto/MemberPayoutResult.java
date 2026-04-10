package server.batch.allocation.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import server.batch.allocation.entity.Account;

@Getter
@Builder
@ToString
public class MemberPayoutResult {

    private Long memberId;          // 회원 ID
    private Account account;        // 회원 계좌 (Writer에서 배당금 입금 시 사용)
    private long payoutAmount;      // 회원 배당금 (allocationAvg * holdingQuantity)
    private long holdingQuantity;   // 회원 보유 토큰 수량
}

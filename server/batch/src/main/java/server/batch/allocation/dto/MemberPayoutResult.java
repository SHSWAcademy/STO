package server.batch.allocation.dto;

import lombok.Builder;
import lombok.Getter;
import server.batch.allocation.entity.Account;

@Getter
@Builder
public class MemberPayoutResult {

    private Long memberId;
    private Account account;
    private long payoutAmount;
    private long holdingQuantity;
}

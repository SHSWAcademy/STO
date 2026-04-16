package server.main.myAccount.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import server.main.allocation.entity.AllocationPayout;
import server.main.token.entity.Token;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class DividendHistoryResponse {
    private Long allocationPayoutId;
    private String tokenName;
    private String tokenSymbol;
    private Long holdingQuantity;
    private Long perTokenAmount;   // memberIncome / holdingQuantity 로 계산
    private Long memberIncome;
    private int settlementYear;
    private int settlementMonth;
    private LocalDateTime createdAt;

    public static DividendHistoryResponse from(AllocationPayout payout, Token token, int settlementYear, int settlementMonth) {
        return new DividendHistoryResponse(
                payout.getAllocationPayoutId(),
                token.getTokenName(),
                token.getTokenSymbol(),
                payout.getHoldingQuantity(),
                payout.getHoldingQuantity() != null && payout.getHoldingQuantity() > 0
                        ? payout.getMemberIncome() / payout.getHoldingQuantity()
                        : 0L, // 주당 배당금 계산
                payout.getMemberIncome(),
                settlementYear,
                settlementMonth,
                payout.getCreatedAt()
        );
    }

}

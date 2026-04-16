package server.main.myAccount.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import server.main.member.entity.MemberTokenHolding;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@AllArgsConstructor
public class PortfolioResponse {

    private Long tokenId;
    private String tokenName;
    private String tokenSymbol;
    private Long quantity;
    private BigDecimal avgBuyPrice;
    private Long currentPrice;
    private Long evaluationAmount; // 평가금액 = currentPrice * quantity
    private Long profit; // 손익 = evaluationAmount - (avgBuyPrice * quantity)
    private BigDecimal profitRate; // 수익률 = profit / (avgBuyPrice * quantity) * 100

    public static PortfolioResponse from(MemberTokenHolding memberTokenHolding) {
        Long quantity = memberTokenHolding.getCurrentQuantity();
        Long currentPrice = memberTokenHolding.getToken().getCurrentPrice();
        BigDecimal avgBuyPrice = memberTokenHolding.getAvgBuyPrice();

        long evaluationAmount = currentPrice * quantity;
        long investedAmount = avgBuyPrice.multiply(BigDecimal.valueOf(quantity))
                .setScale(0, RoundingMode.HALF_UP).longValue();
        long profit = evaluationAmount - investedAmount;
        BigDecimal profitRate = investedAmount == 0 ? BigDecimal.ZERO :
                BigDecimal.valueOf(profit)
                .divide(BigDecimal.valueOf(investedAmount), 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));

        return new PortfolioResponse(
                memberTokenHolding.getToken().getTokenId(),
                memberTokenHolding.getToken().getTokenName(),
                memberTokenHolding.getToken().getTokenSymbol(),
                quantity,
                avgBuyPrice,
                currentPrice,
                evaluationAmount,
                profit,
                profitRate
        );
    }
}

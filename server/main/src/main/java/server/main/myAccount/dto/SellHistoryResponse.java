package server.main.myAccount.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class SellHistoryResponse {
    private Long tradeId;
    private String tokenName;
    private String tokenSymbol;
    private Long tradeQuantity;
    private Long bankingAmount;   // totalTradePrice - feeAmount
    private LocalDateTime executedAt;

    public SellHistoryResponse(Long tradeId, String tokenName, String tokenSymbol,
                               Long tradeQuantity, Long totalTradePrice, Long feeAmount,
                               LocalDateTime executedAt) {
        this.tradeId = tradeId;
        this.tokenName = tokenName;
        this.tokenSymbol = tokenSymbol;
        this.tradeQuantity = tradeQuantity;
        this.bankingAmount = totalTradePrice - feeAmount;
        this.executedAt = executedAt;
    }
}

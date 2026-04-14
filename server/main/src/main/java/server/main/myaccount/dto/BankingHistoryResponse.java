package server.main.myaccount.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import server.main.member.entity.MemberBank;
import server.main.member.entity.TxStatus;
import server.main.member.entity.TxType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class BankingHistoryResponse {

    private Long bankingId;
    private Long bankingAmount;
    private Long balanceSnapshot;
    private TxType txType;
    private TxStatus txStatus;
    private LocalDateTime createdAt;

    public static BankingHistoryResponse from(MemberBank memberBank) {
        return new BankingHistoryResponse(
                memberBank.getBankingId(),
                memberBank.getBankingAmount(),
                memberBank.getBalanceSnapshot(),
                memberBank.getTxType(),
                memberBank.getTxStatus(),
                memberBank.getCreatedAt()
        );
    }
}

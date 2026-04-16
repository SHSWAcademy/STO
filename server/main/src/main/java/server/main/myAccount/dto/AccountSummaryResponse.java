package server.main.myAccount.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountSummaryResponse {
    private Long thisMonthDividend;    // 이번달 배당금 합산
    private Long thisMonthSellProfit;  // 이번달 판매수익 합산
    private Long thisMonthTotal;       // 위 둘의 합산
}

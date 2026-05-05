package server.main.myAccount.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountBalanceResponse {
    private String accountNumber;
    private Long availableBalance;
    private Long lockedBalance;
}

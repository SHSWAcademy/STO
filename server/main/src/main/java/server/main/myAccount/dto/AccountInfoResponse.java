package server.main.myAccount.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AccountInfoResponse {

    private String memberName;
    private String email;
    private String walletAddress;
    private String accountNumber;
}

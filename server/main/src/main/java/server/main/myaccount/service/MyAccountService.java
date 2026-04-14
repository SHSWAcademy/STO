package server.main.myaccount.service;

import org.springframework.data.domain.Page;
import server.main.myaccount.dto.DepositRequest;
import server.main.myaccount.dto.WithdrawRequest;

public interface MyAccountService {

    void deposit(DepositRequest depositRequest);
    void withdraw(WithdrawRequest withdrawRequest);

}

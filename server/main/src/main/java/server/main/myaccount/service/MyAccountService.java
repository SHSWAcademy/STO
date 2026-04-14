package server.main.myaccount.service;

import server.main.myaccount.dto.AccountBalanceResponse;
import server.main.myaccount.dto.DepositRequest;
import server.main.myaccount.dto.PortfolioResponse;
import server.main.myaccount.dto.WithdrawRequest;

import java.util.List;

public interface MyAccountService {

    void deposit(DepositRequest depositRequest);
    void withdraw(WithdrawRequest withdrawRequest);

    AccountBalanceResponse getBalance();

    List<PortfolioResponse> getPortfolio();

}

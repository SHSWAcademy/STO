package server.main.myaccount.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.main.myaccount.dto.*;

import java.util.List;

public interface MyAccountService {

    void deposit(DepositRequest depositRequest);
    void withdraw(WithdrawRequest withdrawRequest);

    AccountBalanceResponse getBalance();

    List<PortfolioResponse> getPortfolio();

    Page<BankingHistoryResponse> getBankingHistory(Pageable pageable);

}

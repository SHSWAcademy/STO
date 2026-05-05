package server.main.myAccount.service;

import server.main.myAccount.dto.AccountBalanceResponse;
import server.main.myAccount.dto.DepositRequest;
import server.main.myAccount.dto.PortfolioResponse;
import server.main.myAccount.dto.VerifyAccountPasswordRequest;
import server.main.myAccount.dto.WithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.main.member.entity.TxType;
import server.main.myAccount.dto.*;

import java.util.List;

public interface MyAccountService {

    void deposit(DepositRequest depositRequest);
    void withdraw(WithdrawRequest withdrawRequest);

    AccountBalanceResponse getBalance();

    List<PortfolioResponse> getPortfolio();

    void verifyAccountPassword(VerifyAccountPasswordRequest request);

    Page<BankingHistoryResponse> getBankingHistory(List<TxType> txTypes, Pageable pageable);

    Page<OrderHistoryResponse> getOrderHistory(String orderTab, Pageable pageable);

    Page<DividendHistoryResponse> getDividendHistory(int year, Integer month, Pageable pageable);

    Long getDividendTotal(int year);

    AccountSummaryResponse getAccountSummary(int year, int month);

    Page<SellHistoryResponse> getSellHistory(int year, int month, Pageable pageable);

    AccountInfoResponse getAccountInfo();


}

package server.main.myaccount.service;

import server.main.myaccount.dto.AccountBalanceResponse;
import server.main.myaccount.dto.DepositRequest;
import server.main.myaccount.dto.PortfolioResponse;
import server.main.myaccount.dto.VerifyAccountPasswordRequest;
import server.main.myaccount.dto.WithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import server.main.member.entity.TxType;
import server.main.myaccount.dto.*;

import java.util.List;

public interface MyAccountService {

    void deposit(DepositRequest depositRequest);
    void withdraw(WithdrawRequest withdrawRequest);

    AccountBalanceResponse getBalance();

    List<PortfolioResponse> getPortfolio();

    void verifyAccountPassword(VerifyAccountPasswordRequest request);

    Page<BankingHistoryResponse> getBankingHistory(List<TxType> txTypes, Pageable pageable);

    Page<OrderHistoryResponse> getOrderHistory(String orderTab, Pageable pageable);

    Page<DividendHistoryResponse> getDividendHistory(int year, Pageable pageable);

    Long getDividendTotal(int year);

    AccountSummaryResponse getAccountSummary(Integer year, Integer month);

    Page<SellHistoryResponse> getSellHistory(int year, int month, Pageable pageable);


}

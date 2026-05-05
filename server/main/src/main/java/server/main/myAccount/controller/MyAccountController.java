package server.main.myAccount.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.myAccount.dto.AccountBalanceResponse;
import server.main.myAccount.dto.DepositRequest;
import server.main.myAccount.dto.PortfolioResponse;
import server.main.myAccount.dto.VerifyAccountPasswordRequest;
import server.main.myAccount.dto.WithdrawRequest;
import server.main.member.entity.TxType;
import server.main.myAccount.dto.*;
import server.main.myAccount.service.MyAccountService;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/myAccount")
@RequiredArgsConstructor
public class MyAccountController {

    private final MyAccountService myAccountService;

    @PostMapping("/deposit")
    public ResponseEntity<Void> deposit(@RequestBody @Valid DepositRequest depositRequest) {
        myAccountService.deposit(depositRequest);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/withdraw")
    public ResponseEntity<Void> withdraw(@RequestBody @Valid WithdrawRequest withdrawRequest) {
        myAccountService.withdraw(withdrawRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/summary")
    public ResponseEntity<AccountSummaryResponse> getAccountSummary(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        int resolvedMonth = (month != null) ? month : LocalDate.now().getMonthValue();

        if (resolvedYear < 2000 || resolvedYear > 2100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ResponseEntity.ok(myAccountService.getAccountSummary(resolvedYear, resolvedMonth));
    }

    @GetMapping("/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance() {
        return ResponseEntity.ok(myAccountService.getBalance());
    }

    @GetMapping("/portfolio")
    public ResponseEntity<List<PortfolioResponse>> getPortfolio() {
        return ResponseEntity.ok(myAccountService.getPortfolio());
    }

    @PostMapping("/verify-password")
    public ResponseEntity<Void> verifyPassword(@RequestBody @Valid VerifyAccountPasswordRequest request) {
        myAccountService.verifyAccountPassword(request);
        return ResponseEntity.ok().build();
    }
    @GetMapping("/history")
    public ResponseEntity<Page<BankingHistoryResponse>> getBankingHistory(
            @RequestParam(required = false) List<TxType> txTypes,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(myAccountService.getBankingHistory(txTypes, pageable));
    }

    @GetMapping("/orders")
    public ResponseEntity<Page<OrderHistoryResponse>> getOrderHistory(
            @RequestParam(defaultValue = "all") String orderTab,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(myAccountService.getOrderHistory(orderTab, pageable));
    }

    @GetMapping("/dividends")
    public ResponseEntity<Page<DividendHistoryResponse>> getDividendHistory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        int resolvedYear = (year != null) ? year : Year.now().getValue();
        if (resolvedYear < 2000 || resolvedYear > 2100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (month != null && (month < 1 || month > 12)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ResponseEntity.ok(myAccountService.getDividendHistory(resolvedYear, month, pageable));
    }

    @GetMapping("/dividends/total")
    public ResponseEntity<Long> getDividendTotal(
            @RequestParam(required = false) Integer year) {
        int resolvedYear = (year != null) ? year : Year.now().getValue();
        return ResponseEntity.ok(myAccountService.getDividendTotal(resolvedYear));
    }

    @GetMapping("/sell-history")
    public ResponseEntity<Page<SellHistoryResponse>> getSellHistory(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @PageableDefault(size = 50) Pageable pageable) {
        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        int resolvedMonth = (month != null) ? month : LocalDate.now().getMonthValue();

        if (resolvedYear < 2000 || resolvedYear > 2100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (resolvedMonth < 1 || resolvedMonth > 12) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
        return ResponseEntity.ok(myAccountService.getSellHistory(resolvedYear, resolvedMonth, pageable));
    }


    @GetMapping("/info")
    public ResponseEntity<AccountInfoResponse> getAccountInfo() {
        return ResponseEntity.ok(myAccountService.getAccountInfo());
    }



}

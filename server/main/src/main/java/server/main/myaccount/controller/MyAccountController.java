package server.main.myaccount.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.main.myaccount.dto.AccountBalanceResponse;
import server.main.myaccount.dto.DepositRequest;
import server.main.myaccount.dto.PortfolioResponse;
import server.main.myaccount.dto.VerifyAccountPasswordRequest;
import server.main.myaccount.dto.WithdrawRequest;
import server.main.member.entity.TxType;
import server.main.myaccount.dto.*;
import server.main.myaccount.service.MyAccountService;

import java.time.Year;
import java.util.List;

@RestController
@RequestMapping("/api/myaccount")
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
    public ResponseEntity<AccountSummaryResponse> getAccountSummary() {
        return ResponseEntity.ok(myAccountService.getAccountSummary());
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
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        int resolvedYear = (year != null) ? year : Year.now().getValue();
        return ResponseEntity.ok(myAccountService.getDividendHistory(resolvedYear, pageable));
    }

    @GetMapping("/dividends/total")
    public ResponseEntity<Long> getDividendTotal(
            @RequestParam(required = false) Integer year) {
        int resolvedYear = (year != null) ? year : Year.now().getValue();
        return ResponseEntity.ok(myAccountService.getDividendTotal(resolvedYear));
    }





}

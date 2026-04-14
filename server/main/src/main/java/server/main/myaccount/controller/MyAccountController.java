package server.main.myaccount.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.main.myaccount.dto.AccountBalanceResponse;
import server.main.myaccount.dto.DepositRequest;
import server.main.myaccount.dto.PortfolioResponse;
import server.main.myaccount.dto.VerifyAccountPasswordRequest;
import server.main.myaccount.dto.WithdrawRequest;
import server.main.myaccount.service.MyAccountService;

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
}

package server.main.myaccount.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.security.CustomUserPrincipal;
import server.main.member.entity.*;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.MemberBankRepository;
import server.main.member.repository.MemberTokenHoldingRepository;
import server.main.myaccount.dto.AccountBalanceResponse;
import server.main.myaccount.dto.DepositRequest;
import server.main.myaccount.dto.PortfolioResponse;
import server.main.myaccount.dto.VerifyAccountPasswordRequest;
import server.main.myaccount.dto.WithdrawRequest;
import server.main.myaccount.dto.*;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MyAccountServiceImpl implements MyAccountService{

    private final AccountRepository accountRepository;
    private final MemberBankRepository memberBankRepository;
    private final MemberTokenHoldingRepository memberTokenHoldingRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void deposit(DepositRequest depositRequest) {

        Long memberId = ((CustomUserPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal()).getId();

        Account account = accountRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.deposit(depositRequest.getAmount());

        MemberBank banking = MemberBank.builder()
                .account(account)
                .bankingAmount(depositRequest.getAmount())
                .txType(TxType.DEPOSIT)
                .txStatus(TxStatus.SUCCESS)
                .balanceSnapshot(account.getAvailableBalance())
                .build();

        memberBankRepository.save(banking);
    }

    @Override
    public void withdraw(WithdrawRequest withdrawRequest) {
        Long memberId = ((CustomUserPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal()).getId();

        Account account = accountRepository.findWithLockByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        account.withdraw(withdrawRequest.getAmount());

        MemberBank banking = MemberBank.builder()
                .account(account)
                .bankingAmount(withdrawRequest.getAmount())
                .txType(TxType.WITHDRAWAL)
                .txStatus(TxStatus.SUCCESS)
                .balanceSnapshot(account.getAvailableBalance())
                .build();

        memberBankRepository.save(banking);
    }

    @Override
    @Transactional(readOnly = true)
    public AccountBalanceResponse getBalance() {
        Long memberId = ((CustomUserPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal()).getId();

        Account account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        return new AccountBalanceResponse(
                account.getAccountNumber(),
                account.getAvailableBalance(),
                account.getLockedBalance()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPortfolio() {
        Long memberId = ((CustomUserPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal()).getId();

        return memberTokenHoldingRepository.findAllByMemberId(memberId)
                .stream()
                .filter(h -> h.getCurrentQuantity() > 0)
                .map(PortfolioResponse :: from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyAccountPassword(VerifyAccountPasswordRequest request) {
    public Page<BankingHistoryResponse> getBankingHistory(List<TxType> txTypes, Pageable pageable) {
        Long memberId = ((CustomUserPrincipal) SecurityContextHolder
                .getContext().getAuthentication().getPrincipal()).getId();

        Account account = accountRepository.findByMemberId(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND));

        if (!passwordEncoder.matches(request.getAccountPassword(), account.getAccountPassword())) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        if (txTypes == null || txTypes.isEmpty()) {
            return memberBankRepository.findByAccount(account, pageable)
                    .map(BankingHistoryResponse::from);
        }

        return memberBankRepository.findByAccountAndTxTypeIn(account, txTypes, pageable)
                .map(BankingHistoryResponse::from);
    }
}

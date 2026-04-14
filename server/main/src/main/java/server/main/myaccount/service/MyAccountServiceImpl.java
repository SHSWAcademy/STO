package server.main.myaccount.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.security.CustomUserPrincipal;
import server.main.member.entity.*;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.MemberBankRepository;
import server.main.myaccount.dto.DepositRequest;
import server.main.myaccount.dto.WithdrawRequest;

@Service
@RequiredArgsConstructor
@Transactional
public class MyAccountServiceImpl implements MyAccountService{

    private final AccountRepository accountRepository;
    private final MemberBankRepository memberBankRepository;

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
}

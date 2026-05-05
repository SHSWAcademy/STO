package server.main.myAccount.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.util.BaseEntity;
import server.main.member.entity.Member;

@Entity
@Getter
@Table(name = "ACCOUNTS")
@NoArgsConstructor
public class Account extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "account_id")
    private Long accountId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    private String accountNumber;

    private String accountPassword;

    private Long availableBalance;

    private Long lockedBalance;

    public static Account create(Member member, String accountNumber, String encodedAccountPassword) {
        Account account = new Account();
        account.member = member;
        account.accountNumber = accountNumber;
        account.accountPassword = encodedAccountPassword;
        account.availableBalance = 0L;
        account.lockedBalance = 0L;
        return account;
    }

    // 매수 호가 시 구매력 차감
    public void lockBalance(Long amount) {
        this.availableBalance -= amount;
        this.lockedBalance += amount;
    }

    // 매수 호가 수정 시 기존 주문 금액 복구 후 수정 주문 금액 반영
    public void relockBalance(Long oldAmount, Long updateAmount) {
        this.availableBalance += oldAmount;
        this.lockedBalance -= oldAmount;

        this.availableBalance -= updateAmount;
        this.lockedBalance += updateAmount;
    }

    public void cancelOrder(Long orderAmount) {
        this.availableBalance += orderAmount;
        this.lockedBalance -= orderAmount;
    }

    // 매수 체결 시 묶인 금액 차감 + 차액 환급 (주문가 > 체결가인 경우)
    public void settleBuyTrade(Long tradeAmount, Long lockedAmount, Long feeAmount) {
        this.lockedBalance -= lockedAmount;
        this.availableBalance += (lockedAmount - tradeAmount - feeAmount);
    }

    // 매도 체결 시 매도 대금 수령
    public void settleSellTrade(Long tradeAmount, Long feeAmount) {
        this.availableBalance += (tradeAmount - feeAmount);
    }

    // 입금
    public void deposit(Long amount) {
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        this.availableBalance += amount;
    }

    // 출금
    public void withdraw(Long amount) {

        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }

        if (this.availableBalance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        this.availableBalance -= amount;
    }
}
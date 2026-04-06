package server.main.member.entity;

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
import server.main.global.util.BaseEntity;

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
        account.member =  member;
        account.accountNumber = accountNumber;
        account.accountPassword = encodedAccountPassword;
        account.availableBalance = 0L;
        account.lockedBalance  =0L;
        return account;
    // 매수 호가 시 구매력 차감
    public void lockBalance(Long amount) {
        this.availableBalance -= amount;
        this.lockedBalance += amount;
    }

    public void relockBalance(Long oldAmount, Long updateAmount) {
        this.availableBalance += oldAmount;
        this.lockedBalance -= oldAmount;

        this.availableBalance -= updateAmount;
        this.lockedBalance += updateAmount;
    }
}

package server.main.member.entity;

import jakarta.persistence.*;
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
}

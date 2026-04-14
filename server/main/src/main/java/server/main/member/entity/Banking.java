package server.main.member.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "bankings")
public class Banking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bankingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id")
    private Account account;

    @Enumerated(EnumType.STRING)
    private TxType txType;

    @Enumerated(EnumType.STRING)
    private TxStatus txStatus;

    private Long bankingAmount;
    private Long balanceSnapshot;
    private LocalDateTime createdAt;

    @Builder
    public Banking(Account account, TxType txType, TxStatus txStatus,
                   Long bankingAmount, Long balanceSnapshot) {
        this.account = account;
        this.txType = txType;
        this.txStatus = txStatus;
        this.bankingAmount = bankingAmount;
        this.balanceSnapshot = balanceSnapshot;
        this.createdAt = LocalDateTime.now();
    }
}

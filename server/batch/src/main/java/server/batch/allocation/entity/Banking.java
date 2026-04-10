package server.batch.allocation.entity;

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

    private Long accountId;
    @Enumerated(value = EnumType.STRING)
    private TxType txType;

    @Enumerated(value = EnumType.STRING)
    private TxStatus txStatus;
    private Long bankingAmount;
    private Long balanceSnapshot;
    private LocalDateTime createdAt;

    @Builder
    public Banking(Long accountId, TxType txType, TxStatus txStatus,
                   Long bankingAmount, Long balanceSnapshot) {
        this.accountId = accountId;
        this.txType = txType;
        this.txStatus = txStatus;
        this.bankingAmount = bankingAmount;
        this.balanceSnapshot = balanceSnapshot;
        this.createdAt = LocalDateTime.now();
    }
}

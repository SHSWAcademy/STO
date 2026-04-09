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
    private String txType;
    private String txStatus;
    private Long bankingAmount;
    private Long balanceSnapshot;
    private LocalDateTime createdAt;

    @Builder
    public Banking(Long accountId, String txType, String txStatus,
                   Long bankingAmount, Long balanceSnapshot) {
        this.accountId = accountId;
        this.txType = txType;
        this.txStatus = txStatus;
        this.bankingAmount = bankingAmount;
        this.balanceSnapshot = balanceSnapshot;
        this.createdAt = LocalDateTime.now();
    }
}

package server.main.member.entity;


import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;


@Entity
@Getter
@Table(name = "BANKINGS")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MemberBank {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long bankingId;

    private Long bankingAmount;

    private Long balanceSnapshot;

    @Enumerated(value = EnumType.STRING)
    private TxType txType;

    @Enumerated(value = EnumType.STRING)
    private TxStatus txStatus;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    private Account account;

    @Builder
    public MemberBank(Long bankingAmount, TxType txType, TxStatus txStatus, Long balanceSnapshot) {
        this.bankingAmount = bankingAmount;
        this.txType = txType;
        this.txStatus = txStatus;
        this.balanceSnapshot = balanceSnapshot;
    }
}

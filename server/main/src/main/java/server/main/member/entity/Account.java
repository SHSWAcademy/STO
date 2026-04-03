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
}

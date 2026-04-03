package server.main.member.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "WALLETS")
@NoArgsConstructor
public class Wallet extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long wallet_id;

    private String walletAddress;

    @Enumerated(EnumType.STRING)
    private WalletType walletType;
    @Enumerated(EnumType.STRING)
    private WalletStatus walletStatus;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
}

package server.main.asset.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "ASSET_BANKINGS")
@NoArgsConstructor
public class AssetBanking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetBankingId;        // 계죄 입출금ID
    private Long assetBankingAmount;    // 계좌 입출금 금액
    private LocalDateTime createdAt;    // 시각

    @Enumerated(EnumType.STRING)
    @Column(name = "asset_banking_direction")
    private AssetBankingDirection direction;    // 입금 / 출금
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_banking_type")
    private AssetBankingType type;      // 입출금 유형

    // 계좌랑 연관 N : 1
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_account_id")
    private AssetAccount assetAccount;

    // 저장될때 현재시간 자동
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }

    // 초기 금액 입/출금
    @Builder
    public AssetBanking (Long assetBankingAmount, AssetAccount assetAccount, AssetBankingDirection direction, AssetBankingType type) {
        this.assetBankingAmount = assetBankingAmount;
        this.assetAccount = assetAccount;
        this.direction = direction;
        this.type = type;
    }

}

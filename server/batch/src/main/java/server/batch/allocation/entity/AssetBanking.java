package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "asset_bankings")
public class AssetBanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetBankingId;     // 부동산 계좌 입출금 내역

    private Long assetAccountId;     // 부동산계좌ID
    private Long assetBankingAmount; // 계좌 입출금 금액
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_banking_direction")
    private AssetBankingDirection direction;    // 입금 / 출금
    @Enumerated(EnumType.STRING)
    @Column(name = "asset_banking_type")
    private AssetBankingType type;      // 입출금 유형
    private LocalDateTime createdAt;


    @Builder
    public AssetBanking(Long assetAccountId, Long assetBankingAmount,
                        AssetBankingDirection direction, AssetBankingType type) {
        this.assetAccountId = assetAccountId;
        this.assetBankingAmount = assetBankingAmount;
        this.direction = direction;
        this.type = type;
        this.createdAt = LocalDateTime.now();
    }
}

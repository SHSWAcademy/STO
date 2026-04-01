package server.main.asset.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "ASSET_ACCOUNTS")
@NoArgsConstructor
public class AssetAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetAccountId;

    private Long assetAccountBalance;

    // 연관관계 필요
}

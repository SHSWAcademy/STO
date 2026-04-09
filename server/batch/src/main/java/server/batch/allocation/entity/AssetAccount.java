package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "asset_accounts")
public class AssetAccount {

    @Id
    private Long assetAccountId; //부동산ID
    private Long assetId;   // 자산ID
    private Long assetAccountBalance; // 출금가능 잔고

    // 출금 시
    public void withdraw(long amount) {
        this.assetAccountBalance -= amount;
    }
}

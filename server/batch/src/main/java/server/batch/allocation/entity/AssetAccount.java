package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "asset_accounts")
public class AssetAccount {

    @Id
    private Long assetAccountId; //부동산ID
    private Long assetId;   // 자산ID
    private Long assetAccountBalance; // 출금가능 잔고
    private Long accumulatedRemainder; // 배당 잔여금
    private LocalDateTime updatedAt;

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // 출금 시
    public void withdraw(long amount) {
        this.assetAccountBalance -= amount;
    }

    // 배당 잔여금 업데이트
    public void updateAccumulatedRemainder(long remainder) {
        this.accumulatedRemainder = remainder;
    }
}

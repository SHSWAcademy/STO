package server.main.asset.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "ASSET_ACCOUNTS")
@NoArgsConstructor
public class AssetAccount extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetAccountId;        // 계좌ID
    private Long assetAccountBalance;   // 출금가능한 잔고
    private Long assetId;               // 자산ID

    // 초기 자산 계좌 생성용
    @Builder
    public AssetAccount (Long assetId) {
        this.assetAccountBalance = 0L;
        this.assetId = assetId;
    }

    // 계좌 입금
    public void deposit(Long assetAccountBalance) {
        this.assetAccountBalance += assetAccountBalance;
    }

    // 계좌 출금
    public void withDraw(Long amount) {
        if (this.assetAccountBalance < amount) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.assetAccountBalance -= amount;
    }
}

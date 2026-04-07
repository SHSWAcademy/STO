package server.main.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.asset.entity.AssetAccount;

public interface AssetAccountRepository extends JpaRepository<AssetAccount, Long> {
    // 자산ID로 계좌 조회
    AssetAccount findByAssetId(Long assetId);
}

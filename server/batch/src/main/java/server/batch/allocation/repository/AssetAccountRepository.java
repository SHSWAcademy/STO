package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.AssetAccount;

import java.util.Optional;

public interface AssetAccountRepository extends JpaRepository<AssetAccount, Long> {

    Optional<AssetAccount> findByAssetId(Long assetId);
}

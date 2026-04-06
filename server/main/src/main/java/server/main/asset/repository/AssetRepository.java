package server.main.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.asset.entity.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    // 자산명 조회
    @Query("SELECT a.assetName FROM Asset a WHERE a.assetId = :assetId")
    String findAssetName(@Param("assetId") Long assetId);
}

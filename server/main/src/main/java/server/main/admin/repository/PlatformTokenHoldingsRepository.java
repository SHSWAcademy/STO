package server.main.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.admin.entity.PlatformTokenHolding;

import java.util.Optional;

public interface PlatformTokenHoldingsRepository extends JpaRepository<PlatformTokenHolding, Long> {

    // 자산 / 토큰 / 플랫폼 보유 토큰 상세조회
    @Query("SELECT p FROM PlatformTokenHolding p JOIN FETCH p.token t JOIN FETCH t.asset WHERE t.asset.assetId = :assetId")
    Optional<PlatformTokenHolding> findWithTokenAndAssetByAssetId(@Param("assetId") Long assetId);
}

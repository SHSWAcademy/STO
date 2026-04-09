package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.batch.allocation.entity.PlatformTokenHolding;

import java.util.Optional;

public interface PlatformTokenHoldingRepository extends JpaRepository<PlatformTokenHolding, Long> {

    @Query("SELECT COALESCE(SUM(p.holdingSupply), 0) FROM PlatformTokenHolding p WHERE p.tokenId = :tokenId")
    Long sumHoldingSupplyByTokenId(Long tokenId);
}

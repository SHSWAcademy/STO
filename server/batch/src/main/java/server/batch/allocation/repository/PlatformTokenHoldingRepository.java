package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.batch.allocation.entity.PlatformTokenHolding;

import java.util.Optional;

public interface PlatformTokenHoldingRepository extends JpaRepository<PlatformTokenHolding, Long> {

    // 플랫폼 보유 수량 조회
    @Query("SELECT p.holdingSupply FROM PlatformTokenHolding p WHERE p.tokenId = :tokenId")
    Long holdingSupplyByTokenId(Long tokenId);
}

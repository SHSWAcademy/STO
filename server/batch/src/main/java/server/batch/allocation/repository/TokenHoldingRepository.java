package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.TokenHolding;

import java.util.List;

public interface TokenHoldingRepository extends JpaRepository<TokenHolding, Long> {

    List<TokenHolding> findByTokenIdAndCurrentQuantityGreaterThan(Long tokenId, Long quantity);
}

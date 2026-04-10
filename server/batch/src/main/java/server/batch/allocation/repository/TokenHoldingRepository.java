package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.TokenHolding;
import server.batch.token.entity.Token;

import java.util.List;

public interface TokenHoldingRepository extends JpaRepository<TokenHolding, Long> {
    // 유저 보유 토큰량 조회 0보다 큰거
    List<TokenHolding> findByTokenIdAndCurrentQuantityGreaterThan(Long tokenId, Long currentQuantity);
}

package server.main.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.admin.entity.PlatformTokenHolding;

public interface PlatformTokenHoldingsRepository extends JpaRepository<PlatformTokenHolding, Long> {
}

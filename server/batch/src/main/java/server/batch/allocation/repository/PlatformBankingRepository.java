package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.PlatformBanking;

public interface PlatformBankingRepository extends JpaRepository<PlatformBanking, Long> {
}

package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.AssetBanking;

public interface AssetBankingRepository extends JpaRepository<AssetBanking, Long> {
}

package server.main.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.asset.entity.AssetBanking;

public interface AssetBankingRepository extends JpaRepository<AssetBanking, Long> {
}

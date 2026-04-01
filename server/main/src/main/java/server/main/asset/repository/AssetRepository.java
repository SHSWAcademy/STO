package server.main.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.asset.entity.Asset;

public interface AssetRepository extends JpaRepository<Asset, Long> {
}

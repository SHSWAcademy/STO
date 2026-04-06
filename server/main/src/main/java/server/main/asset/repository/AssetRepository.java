package server.main.asset.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.main.asset.entity.Asset;


import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {

}

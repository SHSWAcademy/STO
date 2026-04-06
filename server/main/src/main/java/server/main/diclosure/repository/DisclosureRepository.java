package server.main.diclosure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.main.diclosure.entity.Disclosure;

import java.util.Optional;

public interface DisclosureRepository extends JpaRepository<Disclosure, Long> {
    // 자산ID로 조회 (건물 소개 공시 조회)
    @Query("SELECT d FROM Disclosure d WHERE d.assetId = :assetId AND d.disclosureCategory ='BUILDING'")
    Optional<Disclosure> findByAssetIdAndCategory(Long assetId);
}

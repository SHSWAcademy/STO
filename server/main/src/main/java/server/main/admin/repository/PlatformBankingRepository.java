package server.main.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import server.main.admin.entity.PlatformBanking;

import java.util.List;

public interface PlatformBankingRepository extends JpaRepository<server.main.admin.entity.PlatformBanking, Long> {
    // 플랫폼 계좌 입금내역 조회
    @Query("SELECT p FROM PlatformBanking p WHERE p.platformBankingDirection = 'DEPOSIT'")
    List<PlatformBanking> getPlatformBankingList();

}

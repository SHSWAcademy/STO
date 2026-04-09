package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.PlatformAccount;

public interface PlatformAccountRepository extends JpaRepository<PlatformAccount, Long> {

    PlatformAccount findFirstBy();
}

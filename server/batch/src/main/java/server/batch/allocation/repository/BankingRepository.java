package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.Banking;

public interface BankingRepository extends JpaRepository<Banking, Long> {
}

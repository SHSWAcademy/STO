package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Banking;

public interface BankingRepository extends JpaRepository<Banking, Long> {
}

package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Account;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
}

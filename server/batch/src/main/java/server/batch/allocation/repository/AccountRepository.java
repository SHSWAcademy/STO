package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.Account;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByMemberId(Long memberId);
}

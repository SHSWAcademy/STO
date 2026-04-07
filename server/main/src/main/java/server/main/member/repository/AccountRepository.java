package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Account;
import server.main.member.entity.Member;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByMember(Member member);
}

package server.main.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import server.main.member.entity.Account;
import server.main.member.entity.Member;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByMember(Member member);

    // 잔고 변경 전 비관적 락 — 동시 주문/체결 시 잔고 lost update 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockByMember(Member member);
}

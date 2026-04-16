package server.main.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.myAccount.entity.Account;
import server.main.member.entity.Member;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByMember(Member member);

    @Query("SELECT a FROM Account a WHERE a.member.id = :memberId")
    Optional<Account> findByMemberId(@Param("memberId") Long memberId);

    // 잔고 변경 전 비관적 락 — 동시 주문/체결 시 잔고 lost update 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Account> findWithLockByMember(Member member);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.member.memberId = :memberId")
    Optional<Account> findWithLockByMemberId(@Param("memberId") Long memberId);

    Long member(Member member);
}

package server.main.member.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Account;
import server.main.member.entity.MemberBank;

public interface MemberBankRepository extends JpaRepository<MemberBank, Long> {
    Page<MemberBank> findByAccountOrderByCreatedAtDesc(Account account, Pageable pageable);
}

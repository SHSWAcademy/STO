package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Account;
import server.main.member.entity.MemberBank;

import java.util.List;

public interface MemberBankRepository extends JpaRepository<MemberBank, Long> {
    List<MemberBank> findByAccountOrderByCreatedAtDesc(Account account);
}

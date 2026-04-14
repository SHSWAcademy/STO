package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.MemberBank;

public interface BankingRepository extends JpaRepository<MemberBank, Long> {
}

package server.main.member.repository;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import server.main.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailAndIsActiveTrue(String email);
    //SELECT COUNT(*) > 0 FROM members WHERE email = ?
    boolean existsByEmail(String email);
}

package server.main.member.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Member;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailAndIsActiveTrue(String email);
}

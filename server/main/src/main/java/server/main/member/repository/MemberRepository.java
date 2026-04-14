package server.main.member.repository;
import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.repository.query.Param;
import server.main.member.entity.Member;

public interface MemberRepository extends JpaRepository<Member, Long> {
    Optional<Member> findByEmailAndIsActiveTrue(String email);
    //SELECT COUNT(*) > 0 FROM members WHERE email = ?
    boolean existsByEmail(String email);

    // 활성 사용자수 조회 (admin)
    Long countByIsActiveTrue();
    //  신규 가입자 조회
    Long countByCreatedAtBetweenAndIsActiveTrue(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}

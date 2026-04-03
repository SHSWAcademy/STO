package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.token.entity.Token;

import java.util.Optional;

public interface MemberTokenHoldingRepository extends JpaRepository<MemberTokenHolding, Long> {
    // 이 회원이 이 토큰 종목을 몇 개 들고 있는지
    Optional<MemberTokenHolding> findByMemberAndToken(Member findMember, Token token);
}

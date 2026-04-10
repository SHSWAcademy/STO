package server.main.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.token.entity.Token;

import java.util.Optional;

public interface MemberTokenHoldingRepository extends JpaRepository<MemberTokenHolding, Long> {
    // 이 회원이 이 토큰 종목을 몇 개 들고 있는지
    Optional<MemberTokenHolding> findByMemberAndToken(Member findMember, Token token);

    // 신규 보유 레코드 생성 전 조회 — 동시 insert로 인한 유니크 제약 위반 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberTokenHolding> findWithLockByMemberAndToken(Member member, Token token);
}

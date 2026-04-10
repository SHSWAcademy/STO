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

    // 기존 보유 레코드가 있을 때 비관적 락으로 조회 — 동시 체결 시 lost update 방지
    // 행이 없으면 잠글 대상이 없으므로 동시 insert 경쟁은 유니크 제약(uq_token_holdings)으로 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberTokenHolding> findWithLockByMemberAndToken(Member member, Token token);
}

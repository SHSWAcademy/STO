package server.main.member.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.token.entity.Token;

import java.util.List;
import java.util.Optional;

public interface MemberTokenHoldingRepository extends JpaRepository<MemberTokenHolding, Long> {
    // 이 회원이 이 토큰 종목을 몇 개 들고 있는지
    Optional<MemberTokenHolding> findByMemberAndToken(Member findMember, Token token);

    @Query("SELECT h FROM MemberTokenHolding h WHERE h.member.id = :memberId AND h.token.id = :tokenId")
    Optional<MemberTokenHolding> findByMemberIdAndTokenId(@Param("memberId") Long memberId, @Param("tokenId") Long tokenId);

    // 특정 토큰을 1주 이상 보유한 멤버 전체 조회 (배당 알람용)
    @Query("SELECT h FROM MemberTokenHolding h JOIN FETCH h.member WHERE h.token.id = :tokenId AND h.currentQuantity > 0")
    List<MemberTokenHolding> findHoldersByTokenId(@Param("tokenId") Long tokenId);

    // 기존 보유 레코드가 있을 때 비관적 락으로 조회 — 동시 체결 시 lost update 방지
    // 행이 없으면 잠글 대상이 없으므로 동시 insert 경쟁은 유니크 제약(uq_token_holdings)으로 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<MemberTokenHolding> findWithLockByMemberAndToken(Member member, Token token);

    @Query("SELECT h FROM MemberTokenHolding h JOIN FETCH h.token WHERE h.member.memberId = :memberId")
    List<MemberTokenHolding> findAllByMemberId(@Param("memberId") Long memberId);
}

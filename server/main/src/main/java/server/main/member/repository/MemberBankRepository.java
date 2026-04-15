package server.main.member.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.member.entity.Account;
import server.main.member.entity.MemberBank;
import server.main.member.entity.TxType;

import java.time.LocalDateTime;
import java.util.List;

public interface MemberBankRepository extends JpaRepository<MemberBank, Long> {

    Page<MemberBank> findByAccount(Account account, Pageable pageable);

    Page<MemberBank> findByAccountAndTxTypeIn(Account account, List<TxType> txTypes, Pageable pageable);

    // 이번달 특정 txType의 bankingAmount 합산
    @Query("""
      SELECT COALESCE(SUM(b.bankingAmount), 0)
      FROM MemberBank b
      WHERE b.account.member.memberId = :memberId
        AND b.txType = :txType
        AND b.txStatus = 'SUCCESS'
        AND b.createdAt >= :start
        AND b.createdAt < :end
  """)
    Long sumByMemberIdAndTxTypeAndPeriod(
            @Param("memberId") Long memberId,
            @Param("txType") TxType txType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end
    );
}

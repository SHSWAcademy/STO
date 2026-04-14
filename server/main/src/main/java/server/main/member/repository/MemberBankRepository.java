package server.main.member.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Account;
import server.main.member.entity.MemberBank;
import server.main.member.entity.TxType;

import java.util.List;

public interface MemberBankRepository extends JpaRepository<MemberBank, Long> {

    Page<MemberBank> findByAccount(Account account, Pageable pageable);

    Page<MemberBank> findByAccountAndTxTypeIn(Account account, List<TxType> txTypes, Pageable pageable);
}

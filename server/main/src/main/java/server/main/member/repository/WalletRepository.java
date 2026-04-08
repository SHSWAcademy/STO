package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Wallet;
import server.main.member.entity.WalletRole;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    boolean existsByWalletRole(WalletRole walletRole);
}

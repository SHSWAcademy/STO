package server.main.member.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.member.entity.Wallet;
import server.main.member.entity.WalletRole;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    boolean existsByWalletRole(WalletRole walletRole);
    Optional<Wallet> findByWalletRole(WalletRole walletRole);
}

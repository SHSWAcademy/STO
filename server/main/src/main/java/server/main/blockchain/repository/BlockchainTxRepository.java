package server.main.blockchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.blockchain.entity.BlockchainTx;

public interface BlockchainTxRepository extends JpaRepository<BlockchainTx, Long> {
}

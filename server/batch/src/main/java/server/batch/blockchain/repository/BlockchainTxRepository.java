package server.batch.blockchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.blockchain.entity.BlockchainTx;

public interface BlockchainTxRepository extends JpaRepository<BlockchainTx, Long> {
}

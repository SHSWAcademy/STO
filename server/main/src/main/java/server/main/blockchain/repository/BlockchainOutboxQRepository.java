package server.main.blockchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.blockchain.entity.BlockchainOutboxQ;
import server.main.blockchain.entity.QueueStatus;

import java.util.List;

public interface BlockchainOutboxQRepository extends JpaRepository<BlockchainOutboxQ, Long> {
    List<BlockchainOutboxQ> findByStatus(QueueStatus status);
    boolean existsByIdempotencyKey(String idempotencyKey);
}

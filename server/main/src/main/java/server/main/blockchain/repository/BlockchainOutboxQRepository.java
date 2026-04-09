package server.main.blockchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.blockchain.entity.BlockchainOutboxQ;
import server.main.blockchain.entity.QueueStatus;

import java.util.Collection;
import java.util.List;

public interface BlockchainOutboxQRepository extends JpaRepository<BlockchainOutboxQ, Long> {
    List<BlockchainOutboxQ> findByStatus(QueueStatus status);

    List<BlockchainOutboxQ> findByStatusIn(Collection<QueueStatus> statuses);

    boolean existsByIdempotencyKey(String idempotencyKey);

    @Query("SELECT b FROM BlockchainOutboxQ b LEFT JOIN FETCH b.trade LEFT JOIN FETCH b.platformTokenHolding WHERE b.status IN :statuses")
    List<BlockchainOutboxQ> findByStatusInWithFetch(@Param("statuses") Collection<QueueStatus> statuses);
}

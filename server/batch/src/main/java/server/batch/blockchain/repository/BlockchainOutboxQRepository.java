package server.batch.blockchain.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.batch.blockchain.entity.BlockchainOutboxQ;
import server.batch.blockchain.entity.QueueStatus;

import java.util.Collection;
import java.util.List;

public interface BlockchainOutboxQRepository extends JpaRepository<BlockchainOutboxQ, Long> {
    @Query("SELECT b FROM BlockchainOutboxQ b WHERE b.status IN :statuses")
    List<BlockchainOutboxQ> findByStatusIn(@Param("statuses")Collection<QueueStatus> statuses);
}

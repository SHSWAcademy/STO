package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.Common;

public interface CommonRepository extends JpaRepository<Common, Long> {

    Common findFirstBy();
}

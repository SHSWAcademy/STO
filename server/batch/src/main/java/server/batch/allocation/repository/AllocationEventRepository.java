package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.AllocationEvent;

import java.util.List;

public interface AllocationEventRepository extends JpaRepository<AllocationEvent, Long> {

    List<AllocationEvent> findByAllocationBatchStatusFalse();
}

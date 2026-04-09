package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.AllocationPayout;

public interface AllocationPayoutRepository extends JpaRepository<AllocationPayout, Long> {
}

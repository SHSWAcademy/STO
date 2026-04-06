package server.main.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.allocation.entity.AllocationPayout;

public interface AllocationPayoutRepository extends JpaRepository<AllocationPayout, Long> {
}

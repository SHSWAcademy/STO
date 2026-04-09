package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.Commons;

public interface CommonsRepository extends JpaRepository<Commons, Long> {

    Commons findFirstBy();
}

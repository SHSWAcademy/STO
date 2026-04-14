package server.batch.allocation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.allocation.entity.Alarm;

public interface AlarmRepository extends JpaRepository<Alarm, Long> {
}

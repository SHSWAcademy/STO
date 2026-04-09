package server.main.log.orderLog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.log.orderLog.entity.OrderLog;

public interface OrderLogRepository extends JpaRepository<OrderLog, Long> {
}

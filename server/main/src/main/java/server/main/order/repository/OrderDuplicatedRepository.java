package server.main.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.order.entity.OrderDuplicated;

public interface OrderDuplicatedRepository extends JpaRepository<OrderDuplicated, Long> {
}

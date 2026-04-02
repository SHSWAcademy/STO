package server.main.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

}

package server.main.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.order.entity.Order;
import server.main.order.entity.OrderStatus;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

}

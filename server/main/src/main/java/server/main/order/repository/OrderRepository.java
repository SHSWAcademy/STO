package server.main.order.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.order.entity.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.token.tokenId =:tokenId AND o.member.memberId =:memberId "
            + "AND o.orderStatus IN ('OPEN', 'PENDING', 'PARTIAL')")
    List<Order> findPendingOrderByMemberAndToken(@Param("memberId") Long memberId, @Param("tokenId") Long tokenId);

    @Query("SELECT o FROM Order o WHERE o.member.memberId =:memberId AND o.orderId =:orderId")
    Optional<Order> findByMemberIdAndOrderId(Long memberId, Long orderId);
}

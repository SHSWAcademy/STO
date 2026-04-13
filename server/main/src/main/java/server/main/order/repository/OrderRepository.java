package server.main.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import server.main.order.entity.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.token.tokenId =:tokenId AND o.member.memberId =:memberId "
            + "AND o.orderStatus IN ('OPEN', 'PENDING', 'PARTIAL')")
    List<Order> findPendingOrderByMemberAndToken(@Param("memberId") Long memberId, @Param("tokenId") Long tokenId);

    @Query("SELECT o FROM Order o WHERE o.member.memberId =:memberId AND o.orderId =:orderId")
    Optional<Order> findByMemberIdAndOrderId(@Param("memberId") Long memberId, @Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findWithLockById(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.member.memberId = :memberId AND o.orderId = :orderId")
    Optional<Order> findWithLockByMemberIdAndOrderId(@Param("memberId") Long memberId, @Param("orderId") Long orderId);
    
}

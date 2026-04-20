package server.main.order.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import server.main.order.entity.Order;
import server.main.order.entity.OrderStatus;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @Query("SELECT o FROM Order o WHERE o.token.tokenId =:tokenId AND o.member.memberId =:memberId "
            + "AND o.orderStatus IN ('OPEN', 'PENDING', 'PARTIAL')")
    List<Order> findPendingOrderByMemberAndToken(@Param("memberId") Long memberId, @Param("tokenId") Long tokenId);

    @Query("SELECT o FROM Order o WHERE o.member.memberId =:memberId AND o.orderId =:orderId")
    Optional<Order> findByMemberIdAndOrderId(@Param("memberId") Long memberId, @Param("orderId") Long orderId);

    @Query("SELECT o FROM Order o WHERE o.token.tokenId = :tokenId AND o.orderStatus IN ('OPEN', 'PARTIAL') AND o.remainingQuantity > 0")
    List<Order> findOpenAndPartialByTokenId(@Param("tokenId") Long tokenId);

    @Query("SELECT o FROM Order o WHERE o.orderStatus = 'FAILED' AND o.remainingQuantity > 0 ORDER BY o.updatedAt ASC")
    List<Order> findFailedOrdersForRetry();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findWithLockById(@Param("orderId") Long orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.member.memberId = :memberId AND o.orderId = :orderId")
    Optional<Order> findWithLockByMemberIdAndOrderId(@Param("memberId") Long memberId, @Param("orderId") Long orderId);

    // 전체 주문 조회 (페이지네이션)
    @Query("SELECT o FROM Order o WHERE o.member.memberId = :memberId")
    Page<Order> findAllByMemberId(@Param("memberId") Long memberId, Pageable pageable);

    // 상태 필터 포함 조회 (미체결 / 체결)
    @Query("SELECT o FROM Order o WHERE o.member.memberId = :memberId AND o.orderStatus IN :statuses")
    Page<Order> findAllByMemberIdAndStatuses(@Param("memberId") Long memberId, @Param("statuses") List<OrderStatus>
            statuses, Pageable pageable);
    
}

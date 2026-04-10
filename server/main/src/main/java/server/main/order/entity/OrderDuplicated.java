package server.main.order.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders_duplicated")
public class OrderDuplicated {

    @Id
    @Column(name = "order_id")
    private Long orderId;

    private Long memberId;
    private Long tokenId;
    private Long orderPrice;
    private Long orderQuantity;
    private Long filledQuantity;
    private Long remainingQuantity;

    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    private Long orderSequence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime archivedAt;
}

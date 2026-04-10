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
    private LocalDateTime archivedAt; // 주문이 최종 종료되어 duplicated로 들어온 시점
//    createdAt (10:00): 고객이 물건을 주문한 시간
//
//    updatedAt (14:00): 택배 기사님이 배송을 시작한 시간 (상태 변경)
//
//    archivedAt (16:00): 배송이 완전히 끝나서 '완료' 목록으로 넘어간 시간
}

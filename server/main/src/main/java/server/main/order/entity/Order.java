package server.main.order.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import server.main.global.util.BaseEntity;
import server.main.member.entity.Member;
import server.main.token.entity.Token;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Table(name = "ORDERS")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long orderId;

    @Column(nullable = true)
    private Long orderSequence;         // 주문 순서 (match 서버가 부여, 주문 생성 시 null)

    private Long orderPrice;            // 지정가 주문 가격 (호가)

    private Long orderQuantity;         // 처음 요청한 매도 / 매수 수량

    @Column(nullable = false)
    private Long filledQuantity = 0L;   // 체결 수량

    private Long remainingQuantity;     // 미체결 수량

    @Enumerated(EnumType.STRING)
    private OrderType orderType;        // 매도, 매수 여부

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;    // 호가 상태


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id")
    private Token token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 주문 수정 전용 메서드
    public void updateOrder(Long updatePrice, Long updateQuantity) {
        this.orderPrice = updatePrice;
        this.orderQuantity = updateQuantity;
        this.remainingQuantity = updateQuantity - this.filledQuantity;
    }

    public void removeOrder() {
        // updatedAd은 자동으로 값이 채워진다
        this.orderStatus = OrderStatus.CANCELLED; // 주문 취소
    }
}

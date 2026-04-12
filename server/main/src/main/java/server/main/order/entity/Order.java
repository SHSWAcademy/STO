package server.main.order.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.global.util.BaseEntity;
import server.main.member.entity.Member;
import server.main.token.entity.Token;

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
    @Builder.Default
    private Long filledQuantity = 0L;   // 체결 수량

    @Column(nullable = false)
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
    
    public void applyMatchResult(Long filledQuantity, Long remainingQuantity, OrderStatus status) {
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.orderStatus = status;
    }

    // match 서버가 부여한 시간 우선순위 번호 저장
    public void updateSequence(Long sequence) {
        this.orderSequence = sequence;
    }
}

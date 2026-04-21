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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
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
    private Long orderSequence;

    private Long orderPrice;

    private Long orderQuantity;

    @Column(nullable = false)
    @Builder.Default
    private Long filledQuantity = 0L;

    @Column(nullable = false)
    private Long remainingQuantity;

    @Enumerated(EnumType.STRING)
    private OrderType orderType;

    @Enumerated(EnumType.STRING)
    private OrderStatus orderStatus;

    @Column(nullable = false)
    @ColumnDefault("0")
    @Builder.Default
    private Integer retryCount = 0;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String failedMatchResultJson;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id")
    private Token token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public void updateOrder(Long updatePrice, Long updateQuantity) {
        this.orderPrice = updatePrice;
        this.orderQuantity = updateQuantity;
        this.remainingQuantity = updateQuantity - this.filledQuantity;
        this.orderStatus = OrderStatus.PENDING;
    }

    public void restoreOrder(Long originalPrice, Long originalQuantity) {
        this.orderPrice = originalPrice;
        this.orderQuantity = originalQuantity;
        this.remainingQuantity = originalQuantity - this.filledQuantity;
        this.orderStatus = this.filledQuantity > 0 ? OrderStatus.PARTIAL : OrderStatus.OPEN;
    }

    public void markCancelPending() {
        this.orderStatus = OrderStatus.PENDING;
    }

    public void removeOrder() {
        this.orderStatus = OrderStatus.CANCELLED;
        this.retryCount = 0;
        this.failedMatchResultJson = null;
    }

    public void markFailed(String failedMatchResultJson) {
        this.orderStatus = OrderStatus.FAILED;
        this.retryCount = 0;
        this.failedMatchResultJson = failedMatchResultJson;
    }

    public void applyMatchResult(Long filledQuantity, Long remainingQuantity, OrderStatus status) {
        this.filledQuantity = filledQuantity;
        this.remainingQuantity = remainingQuantity;
        this.orderStatus = status;
        this.retryCount = 0;
        this.failedMatchResultJson = null;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }

    public void resetRetryCount() {
        this.retryCount = 0;
    }

    public void updateSequence(Long sequence) {
        this.orderSequence = sequence;
    }
}

package server.match.order.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import server.match.order.entity.OrderType;

@EqualsAndHashCode(of = "orderId")
@Getter
public class Order {
    private final Long orderId;
    private final Long memberId;
    private final Long tokenId;
    private final OrderType orderType;
    private final Long price;
    private Long remainingQuantity; // 남은 수량 (변함)
    private Long sequence;          // 오더북 삽입 시 부여되는 시간 우선순위 번호

    public Order(Long orderId, Long memberId, Long tokenId, OrderType orderType, Long price, Long quantity) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.tokenId = tokenId;
        this.orderType = orderType;
        this.price = price;
        this.remainingQuantity = quantity;
    }

    public void reduceQuantity(Long amount) {
        this.remainingQuantity -= amount;
    }

    // addOrder() 시 OrderBook이 호출 — 시간 우선순위 번호 부여
    public void assignSequence(Long sequence) {
        this.sequence = sequence;
    }

    // 수량 감소 시 in-place 수정 — 오더북 줄 위치(시간 우선순위) 유지
    public void updateQuantity(Long newQuantity) {
        this.remainingQuantity = newQuantity;
    }
    
}

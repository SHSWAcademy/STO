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
    
}

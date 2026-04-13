package server.main.order.dto;

import lombok.Builder;
import lombok.Getter;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;

@Getter
@Builder
public class CancelOrderContext {
    private Long orderId;
    private Long tokenId;
    private OrderType orderType;
    private Long orderPrice;
    private Long remainingQuantity;
    private OrderStatus originalStatus;
}

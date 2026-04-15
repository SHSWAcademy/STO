package server.main.myaccount.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import server.main.order.entity.Order;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderHistoryResponse {

    private Long orderId;
    private String tokenSymbol;
    private String tokenName;
    private OrderType orderType;
    private OrderStatus orderStatus;
    private Long orderPrice;
    private Long orderQuantity;
    private Long filledQuantity;
    private Long remainingQuantity;
    private LocalDateTime createdAt;

    public static OrderHistoryResponse from(Order order) {
        return new OrderHistoryResponse(
                order.getOrderId(),
                order.getToken().getTokenSymbol(),
                order.getToken().getTokenName(),
                order.getOrderType(),
                order.getOrderStatus(),
                order.getOrderPrice(),
                order.getOrderQuantity(),
                order.getFilledQuantity(),
                order.getRemainingQuantity(),
                order.getCreatedAt()
        );
    }


}

package server.main.myAccount.dto;

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
    private Long averageTradePrice;
    private Long executedTradeAmount;
    private Long feeAmount;
    private Long settlementAmount;
    private LocalDateTime createdAt;

    public static OrderHistoryResponse from(Order order, Object[] executionSummary) {
        Long executedTradeAmount = numberAt(executionSummary, 0);
        Long feeAmount = numberAt(executionSummary, 1);
        Long executedQuantity = numberAt(executionSummary, 2);
        Long averageTradePrice = executedQuantity > 0 ? executedTradeAmount / executedQuantity : 0L;
        Long settlementAmount = switch (order.getOrderType()) {
            case BUY -> executedTradeAmount + feeAmount;
            case SELL -> executedTradeAmount - feeAmount;
        };

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
                averageTradePrice,
                executedTradeAmount,
                feeAmount,
                settlementAmount,
                order.getCreatedAt()
        );
    }

    private static Long numberAt(Object[] values, int index) {
        if (values == null || values.length <= index || values[index] == null) {
            return 0L;
        }
        return ((Number) values[index]).longValue();
    }
}

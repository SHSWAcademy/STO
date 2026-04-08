package server.match.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.match.order.entity.OrderType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchOrderRequestDto {
    private Long orderId;
    private Long tokenId;
    private Long memberId;
    private OrderType orderType;   // BUY, SELL
    private Long orderPrice;
    private Long orderQuantity;
}

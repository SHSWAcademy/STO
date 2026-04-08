package server.match.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeExecutionDto {
    private Long counterOrderId;   // 체결된 상대 주문 ID
    private Long counterMemberId;  // 체결된 상대 멤버 ID
    private Long tradePrice;       // 체결가
    private Long tradeQuantity;    // 체결 수량
}

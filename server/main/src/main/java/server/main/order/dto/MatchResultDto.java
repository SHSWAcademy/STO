package server.main.order.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.main.order.entity.OrderStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDto {
    private Long orderId; // 요청 수량 id
    private Long tokenId; // 토큰 id
    private OrderStatus finalStatus; // 최종상태
    private Long filledQuantity; // 체결 된 수량
    private Long remainingQuantity;
    private List <TradeExecutionDto> executions; // 체결 목록
}

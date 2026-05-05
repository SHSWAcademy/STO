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
    private Long orderId;                       // 요청 주문 ID
    private Long tokenId;                       // 토큰 ID
    private Long orderSequence;                 // 오더북 삽입 시 부여된 시간 우선순위 번호
    private OrderStatus finalStatus;            // 최종 상태
    private Long filledQuantity;                // 체결된 수량
    private Long remainingQuantity;             // 남은 수량
    private List<TradeExecutionDto> executions; // 체결 목록
}

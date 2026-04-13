package server.match.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.match.order.entity.OrderStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDto {
    private Long orderId;                       // 요청 주문 ID
    private Long tokenId;                       // 토큰 ID
    private Long orderSequence;                 // 오더북 삽입 시 부여된 시간 우선순위 번호
    private OrderStatus finalStatus;            // 최종 상태 (OPEN, PARTIAL, FILLED)
    private Long filledQuantity;                // 체결된 수량
    private Long remainingQuantity;             // 남은 수량
    private List<TradeExecutionDto> executions; // 체결 목록
    private Long orderSequence;
}

package server.main.order.dto;

import lombok.*;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingOrderResponseDto {
    // 상세 페이지 - 대기 화면에 들어갈 값
    // 생성 또는 상태 변경일 (수정일자), 매수인지 매도인지, 지정 가격, 수량, 총 주문금액
    private Long orderId;
    private OrderType orderType;
    private OrderStatus orderStatus; // 화면엔 없지만 있어야할 듯? (PARTIAL, OPEN 상태 표시)
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long orderPrice;            // 지정가 주문 가격 (호가)
    private Long orderQuantity;         // 화면엔 없지만 리액트에서 계산할 때 사용하게 하기 위해 필요할 듯
    private Long filledQuantity;        // 체결 수량
    private Long remainingQuantity;     // 미체결 수량
}

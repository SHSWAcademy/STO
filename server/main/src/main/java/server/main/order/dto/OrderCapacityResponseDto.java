package server.main.order.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCapacityResponseDto {
    private Long availableBalance;   // 매수 가능 원화 잔고
    private Long availableQuantity;  // 매도 가능 토큰 수량 (없으면 0 으로 화면에 전달)
}

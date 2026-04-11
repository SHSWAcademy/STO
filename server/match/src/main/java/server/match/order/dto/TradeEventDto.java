package server.match.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeEventDto {
    private Long tokenId;          // 어떤 토큰의 체결인지
    private Long tradePrice;       // 체결가
    private Long tradeQuantity;    // 체결 수량
    private boolean isBuy;         // 체결 주도 방향 (매수 주문이 체결 일으켰으면 true)
    private LocalDateTime tradeTime; // 체결 시각
}

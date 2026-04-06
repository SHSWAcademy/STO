package server.main.trade.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TradeResponseDto {
    // 상세 페이지에서 화면에 전달할 주문 체결 완료된 내역
    // 화면에 보여줄 것 : 체결가, 체결 수량, 등락률, 총 거래량, 체결 시간
    // private Long tradeId;                           // 필요한가 ?

    private Long tradePrice;                        // 실제 체결 가격 (1 토큰당 가격)
    private Long tradeQuantity;                     // 실제 체결 수량

    private Double percentageChange;                // 등락률
    private Long totalVolume;                       // 당일 누적 거래량

    private LocalDateTime executedAt;               // 체결 시간
}

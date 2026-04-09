package server.main.candle.dto;

import lombok.*;
import server.main.candle.entity.CandleType;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CandleResponseDto {
    private CandleType candleType;     // WS push 시 어느 주기 봉인지 구분 (REST 응답 시 null)
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    private Double volume;
    private LocalDateTime candleTime;
    private Integer tradeCount;
}

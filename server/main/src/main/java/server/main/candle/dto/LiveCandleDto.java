package server.main.candle.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LiveCandleDto {
    private Long openPrice;
    private Long highPrice;
    private Long lowPrice;
    private Long closePrice;
    private Long volume;
    private int tradeCount;
    private LocalDateTime candleTime;       // 해당 봉이 시작된 시간

    // 체결 발생 시 레디스에서 체결 정보를 받아 현재 봉 갱신
    public void update(Long tradePrice, Long tradeQuantity) {
        this.highPrice  = Math.max(this.highPrice, tradePrice); // 고가 파악
        this.lowPrice   = Math.min(this.lowPrice, tradePrice);  // 저가 파악
        this.closePrice = tradePrice;
        this.volume     += tradeQuantity;
        this.tradeCount += 1;
    }

}

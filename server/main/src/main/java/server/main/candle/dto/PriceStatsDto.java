package server.main.candle.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PriceStatsDto {

    // 1분
    private Double minuteHighPrice;
    private Double minuteLowPrice;

    // 1시간
    private Double hourHighPrice;
    private Double hourLowPrice;

    // 1일 (오전 9시 기준 초기화)
    private Double dayHighPrice;
    private Double dayLowPrice;

    // 1달
    private Double monthHighPrice;
    private Double monthLowPrice;

    // 1년
    private Double yearHighPrice;
    private Double yearLowPrice;
}

package server.main.candle.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.dto.LiveCandle;
import server.main.candle.entity.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CandleMapper {
    CandleResponseDto toDto(CandleMinute candle);
    CandleResponseDto toDto(CandleHour candle);
    CandleResponseDto toDto(CandleDay candle);
    CandleResponseDto toDto(CandleMonth candle);
    CandleResponseDto toDto(CandleYear candle);

    // LiveCandle(메모리) → CandleResponseDto (WS push / 스냅샷 응답용)
    default CandleResponseDto toLiveDto(LiveCandle candle, CandleType type) {
        return CandleResponseDto.builder()
                .candleType(type)
                .openPrice(candle.getOpenPrice())
                .highPrice(candle.getHighPrice())
                .lowPrice(candle.getLowPrice())
                .closePrice(candle.getClosePrice())
                .volume(candle.getVolume())
                .tradeCount(candle.getTradeCount())
                .candleTime(candle.getCandleTime())
                .build();
    }
}

package server.main.candle.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.entity.*;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CandleMapper {
    CandleResponseDto toDto(CandleMinute candle);
    CandleResponseDto toDto(CandleHour candle);
    CandleResponseDto toDto(CandleDay candle);
    CandleResponseDto toDto(CandleMonth candle);
    CandleResponseDto toDto(CandleYear candle);
}

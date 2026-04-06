package server.main.candle.service;

import server.main.candle.dto.CandleResponseDto;
import server.main.candle.entity.CandleType;

import java.util.List;

public interface CandleService {

    List<CandleResponseDto> getCandles(Long tokenId, CandleType type);
}

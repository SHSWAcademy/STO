package server.main.candle.service;

import server.main.candle.dto.PriceStatsDto;

public interface CandleService {
    PriceStatsDto getPriceStats(Long tokenId);
}

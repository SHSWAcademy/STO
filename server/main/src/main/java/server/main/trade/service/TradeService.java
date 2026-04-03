package server.main.trade.service;

import server.main.trade.dto.TradeResponseDto;

import java.util.List;

public interface TradeService {

    List<TradeResponseDto> getTrades(Long tokenId);
}

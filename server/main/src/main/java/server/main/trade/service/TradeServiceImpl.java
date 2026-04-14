package server.main.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.candle.entity.CandleDay;
import server.main.candle.repository.CandleDayRepository;
import server.main.trade.dto.TradeResponseDto;
import server.main.trade.entity.Trade;
import server.main.trade.mapper.TradeMapper;
import server.main.trade.repository.TradeRepository;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class TradeServiceImpl implements TradeService {
    private final TradeRepository tradeRepository;
    private final TradeMapper tradeMapper;
    private final CandleDayRepository candleDayRepository;

    @Override
    public List<TradeResponseDto> getTrades(Long tokenId) {
        List<Trade> trades = tradeRepository.findTradeList(tokenId); // 50개 가져옴

        Long totalVolume = tradeRepository.sumDailyVolume(tokenId); // 오늘 거래된 누적 합 조회

        List<TradeResponseDto> dtos =
                trades.stream().map(tradeMapper::toDto).toList();

        // 등락률 계산 : (체결가 - 전날 종가) / 전날 종가 × 100
        Long yesterdayClose = candleDayRepository.findLatest(tokenId)
                .map(CandleDay::getClosePrice)
                .orElse(null);

        for (TradeResponseDto dto : dtos) {
            if (yesterdayClose != null && yesterdayClose > 0) {
                double change = (double)(dto.getTradePrice() - yesterdayClose) / yesterdayClose * 100;
                dto.setPercentageChange(Math.round(change * 100.0) / 100.0);
            } else {
                dto.setPercentageChange(0.0);
            }
        }

        // 총 체결량
        dtos.forEach(dto -> dto.setTotalVolume(totalVolume));
        return dtos;
    }
}

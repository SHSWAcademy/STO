package server.main.trade.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    @Override
    public List<TradeResponseDto> getTrades(Long tokenId) {
        List<Trade> trades = tradeRepository.findTradeList(tokenId); // 50개 가져옴

        Long totalVolume = tradeRepository.sumDailyVolume(tokenId); // 오늘 거래된 누적 합 조회

        List<TradeResponseDto> dtos =
                trades.stream().map(tradeMapper::toDto).toList();

        // 등락률 계산 : (이번 체결가 - 이전 체결가) / 이전 체결가       곱하기 100
        for (int i = 0; i < dtos.size(); i++) {
            if (i < trades.size() - 1) { // 가장 첫번째 값이 아닐 경우
                long current = trades.get(i).getTradePrice();   // 이번 체결가
                long previous = trades.get(i + 1).getTradePrice();  // 이전 체결가

                double change = (double)(current - previous) / previous * 100;

                dtos.get(i).setPercentageChange(Math.round(change * 100.0) / 100.0);
            } else {
                dtos.get(i).setPercentageChange(0.0); // 첫 번째 체결 등락률 = 0.0
            }
        }

        // 총 체결량
        dtos.forEach(dto -> dto.setTotalVolume(totalVolume));
        return dtos;
    }
}

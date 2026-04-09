package server.main.candle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.candle.dto.LiveCandleDto;
import server.main.candle.entity.*;
import server.main.candle.repository.*;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

@Service
@RequiredArgsConstructor
public class CandleFlushService {

    private final CandleMinuteRepository candleMinuteRepository;
    private final CandleHourRepository   candleHourRepository;
    private final CandleDayRepository    candleDayRepository;
    private final CandleMonthRepository  candleMonthRepository;
    private final CandleYearRepository   candleYearRepository;
    private final TokenRepository        tokenRepository;

    @Transactional
    public void saveToDB(LiveCandleDto dto, Long tokenId, CandleType type) {
        Token token = tokenRepository.getReferenceById(tokenId); // 토큰 프록시로 조회 (실제 쿼리가 나가지 않는다)
        switch (type) {
            case MINUTE -> candleMinuteRepository.save(
                    CandleMinute.builder()
                            .token(token)// 프록시 주입
                            .openPrice(dto.getOpenPrice())
                            .highPrice(dto.getHighPrice())
                            .lowPrice(dto.getLowPrice())
                            .closePrice(dto.getClosePrice())
                            .volume(dto.getVolume())
                            .tradeCount(dto.getTradeCount())
                            .candleTime(dto.getCandleTime())
                            .build()
                    );
            case HOUR -> candleHourRepository.save(
                    CandleHour.builder()
                            .token(token)
                            .openPrice(dto.getOpenPrice())
                            .highPrice(dto.getHighPrice())
                            .lowPrice(dto.getLowPrice())
                            .closePrice(dto.getClosePrice())
                            .volume(dto.getVolume())
                            .tradeCount(dto.getTradeCount())
                            .candleTime(dto.getCandleTime())
                            .build()
                    );
            case DAY -> candleDayRepository.save(
                    CandleDay.builder()
                            .token(token)
                            .openPrice(dto.getOpenPrice())
                            .highPrice(dto.getHighPrice())
                            .lowPrice(dto.getLowPrice())
                            .closePrice(dto.getClosePrice())
                            .volume(dto.getVolume())
                            .tradeCount(dto.getTradeCount())
                            .candleTime(dto.getCandleTime())
                            .build()
                    );
            case MONTH -> candleMonthRepository.save(
                    CandleMonth.builder()
                            .token(token)
                            .openPrice(dto.getOpenPrice())
                            .highPrice(dto.getHighPrice())
                            .lowPrice(dto.getLowPrice())
                            .closePrice(dto.getClosePrice())
                            .volume(dto.getVolume())
                            .tradeCount(dto.getTradeCount())
                            .candleTime(dto.getCandleTime())
                            .build()
                    );
            case YEAR -> candleYearRepository.save(
                    CandleYear.builder()
                            .token(token)
                            .openPrice(dto.getOpenPrice())
                            .highPrice(dto.getHighPrice())
                            .lowPrice(dto.getLowPrice())
                            .closePrice(dto.getClosePrice())
                            .volume(dto.getVolume())
                            .tradeCount(dto.getTradeCount())
                            .candleTime(dto.getCandleTime())
                            .build()
                    );
        }
    }
}

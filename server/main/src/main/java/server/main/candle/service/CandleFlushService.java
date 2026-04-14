package server.main.candle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.candle.dto.LiveCandleDto;
import server.main.candle.entity.CandleType;
import server.main.candle.repository.*;

@Service
@RequiredArgsConstructor
public class CandleFlushService {

    private final CandleMinuteRepository candleMinuteRepository;
    private final CandleHourRepository   candleHourRepository;
    private final CandleDayRepository    candleDayRepository;
    private final CandleMonthRepository  candleMonthRepository;
    private final CandleYearRepository   candleYearRepository;

    @Transactional
    public void saveToDB(LiveCandleDto dto, Long tokenId, CandleType type) {
        // save() 대신 upsert — 서버 재시작 후 같은 구간이 재저장될 때 중복 키 에러 방지
        switch (type) {
            case MINUTE -> candleMinuteRepository.upsert(
                    tokenId, dto.getCandleTime(),
                    dto.getOpenPrice(), dto.getHighPrice(), dto.getLowPrice(), dto.getClosePrice(),
                    dto.getVolume(), dto.getTradeCount());
            case HOUR -> candleHourRepository.upsert(
                    tokenId, dto.getCandleTime(),
                    dto.getOpenPrice(), dto.getHighPrice(), dto.getLowPrice(), dto.getClosePrice(),
                    dto.getVolume(), dto.getTradeCount());
            case DAY -> candleDayRepository.upsert(
                    tokenId, dto.getCandleTime(),
                    dto.getOpenPrice(), dto.getHighPrice(), dto.getLowPrice(), dto.getClosePrice(),
                    dto.getVolume(), dto.getTradeCount());
            case MONTH -> candleMonthRepository.upsert(
                    tokenId, dto.getCandleTime(),
                    dto.getOpenPrice(), dto.getHighPrice(), dto.getLowPrice(), dto.getClosePrice(),
                    dto.getVolume(), dto.getTradeCount());
            case YEAR -> candleYearRepository.upsert(
                    tokenId, dto.getCandleTime(),
                    dto.getOpenPrice(), dto.getHighPrice(), dto.getLowPrice(), dto.getClosePrice(),
                    dto.getVolume(), dto.getTradeCount());
        }
    }
}

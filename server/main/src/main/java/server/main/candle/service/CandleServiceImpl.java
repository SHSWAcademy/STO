package server.main.candle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.candle.dto.PriceStatsDto;
import server.main.candle.repository.*;

import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CandleServiceImpl implements CandleService {

    private final CandleMinuteRepository candleMinuteRepository;
    private final CandleHourRepository   candleHourRepository;
    private final CandleDayRepository    candleDayRepository;
    private final CandleMonthRepository  candleMonthRepository;
    private final CandleYearRepository   candleYearRepository;

    @Override
    public PriceStatsDto getPriceStats(Long tokenId) {
        LocalDateTime now = LocalDateTime.now();

        // 1분: 현재 시각 기준 1분 전
        LocalDateTime minuteFrom = now.minusMinutes(1);

        // 1시간: 현재 시각 기준 1시간 전
        LocalDateTime hourFrom = now.minusHours(1);

        // 1일: 오전 9시 기준 초기화
        LocalDateTime dayFrom = now.getHour() >= 9
                ? now.toLocalDate().atTime(9, 0)
                : now.toLocalDate().minusDays(1).atTime(9, 0);

        // 1달: 현재 시각 기준 1달 전
        LocalDateTime monthFrom = now.minusMonths(1);

        // 1년: 현재 시각 기준 1년 전
        LocalDateTime yearFrom = now.minusYears(1);

        return PriceStatsDto.builder()
                .minuteHighPrice(candleMinuteRepository.findMinuteHighPrice(tokenId, minuteFrom))
                .minuteLowPrice(candleMinuteRepository.findMinuteLowPrice(tokenId, minuteFrom))
                .hourHighPrice(candleHourRepository.findHourHighPrice(tokenId, hourFrom))
                .hourLowPrice(candleHourRepository.findHourLowPrice(tokenId, hourFrom))
                .dayHighPrice(candleDayRepository.findDayHighPrice(tokenId, dayFrom))
                .dayLowPrice(candleDayRepository.findDayLowPrice(tokenId, dayFrom))
                .monthHighPrice(candleMonthRepository.findMonthHighPrice(tokenId, monthFrom))
                .monthLowPrice(candleMonthRepository.findMonthLowPrice(tokenId, monthFrom))
                .yearHighPrice(candleYearRepository.findYearHighPrice(tokenId, yearFrom))
                .yearLowPrice(candleYearRepository.findYearLowPrice(tokenId, yearFrom))
                .build();
    }
}

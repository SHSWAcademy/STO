package server.main.candle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.entity.CandleType;
import server.main.candle.mapper.CandleMapper;
import server.main.candle.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class CandleServiceImpl implements CandleService {

    private final CandleMinuteRepository candleMinuteRepository;
    private final CandleHourRepository candleHourRepository;
    private final CandleDayRepository candleDayRepository;
    private final CandleMonthRepository candleMonthRepository;
    private final CandleYearRepository candleYearRepository;
    private final CandleMapper candleMapper;

    @Override
    public List<CandleResponseDto> getCandles(Long tokenId, CandleType type) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case MINUTE -> candleMinuteRepository.findTop35Before(tokenId, now.truncatedTo(ChronoUnit.MINUTES))
                    .stream()
                    .map(c -> candleMapper.toDto(c))
                    .toList();
            case HOUR -> candleHourRepository.findTop35Before(tokenId, now.truncatedTo(ChronoUnit.HOURS))
                    .stream()
                    .map(candleMapper::toDto)
                    .toList();
            case DAY -> candleDayRepository.findTop35Before(tokenId, now.truncatedTo(ChronoUnit.DAYS))
                    .stream()
                    .map(candleMapper::toDto)
                    .toList();
            case MONTH -> candleMonthRepository.findTop35Before(tokenId, now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS))
                    .stream()
                    .map(c -> candleMapper.toDto(c))
                    .toList();

            case YEAR ->  candleYearRepository.findTop35Before(tokenId, now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS))
                    .stream()
                    .map(c -> candleMapper.toDto(c))
                    .toList();
        };
    }
}

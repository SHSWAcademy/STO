package server.main.candle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.dto.LiveCandleDto;
import server.main.candle.entity.CandleType;
import server.main.candle.mapper.CandleMapper;
import server.main.candle.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
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
    private final CandleLiveManager candleLiveManager;

    private LocalDateTime getDayBucket(LocalDateTime now) {
        return now.getHour() >= 9
                ? now.toLocalDate().atTime(9, 0)
                : now.toLocalDate().minusDays(1).atTime(9, 0);
    }

    @Override
    public List<CandleResponseDto> getCandles(Long tokenId, CandleType type) {
        LocalDateTime now = LocalDateTime.now();
        List<CandleResponseDto> result = new ArrayList<>(switch (type) {
            case MINUTE -> candleMinuteRepository.findTop35Before(tokenId, now.truncatedTo(ChronoUnit.MINUTES))
                    .reversed().stream().map(c -> candleMapper.toDto(c)).toList();
            case HOUR   -> candleHourRepository.findTop35Before(tokenId, now.truncatedTo(ChronoUnit.HOURS))
                    .reversed().stream().map(candleMapper::toDto).toList();
            case DAY    -> candleDayRepository.findTop35Before(tokenId, getDayBucket(now))
                    .reversed().stream().map(candleMapper::toDto).toList();
            case MONTH  -> candleMonthRepository.findTop35Before(tokenId, now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS))
                    .reversed().stream().map(c -> candleMapper.toDto(c)).toList();
            case YEAR   -> candleYearRepository.findTop35Before(tokenId, now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS))
                    .reversed().stream().map(c -> candleMapper.toDto(c)).toList();
        });

        // 현재 진행 중인 봉(메모리) 추가 — DB flush 전이라도 최신 봉이 차트에 표시되도록
        LiveCandleDto snapshot = candleLiveManager.getSnapshot(tokenId, type);
        if (snapshot != null) {
            // DB 마지막 봉과 ts 중복 방지
            LocalDateTime snapshotTs = snapshot.getCandleTime();
            boolean alreadyInDb = !result.isEmpty() && result.getLast().getCandleTime().equals(snapshotTs);
            if (!alreadyInDb) {
                result.add(candleMapper.toLiveDto(snapshot, type));
            }
        }

        return result;
    }
}

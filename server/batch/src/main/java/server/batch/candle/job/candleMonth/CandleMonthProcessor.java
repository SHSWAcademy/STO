package server.batch.candle.job.candleMonth;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleDay;
import server.batch.candle.entity.CandleMonth;
import server.batch.candle.repository.CandleDayRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CandleMonthProcessor implements ItemProcessor<Long, CandleMonth> {

    private final CandleDayRepository candleDayRepository;

    // ItemReader 에서 from ~ to 까지 거래된 토큰 id 리스트 속 tokenId 가 하나씩 넘어온다.
    @Override
    public CandleMonth process(Long tokenId) throws Exception {

        LocalDateTime to = LocalDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS); // to : 이번 달 1일 00:00
        LocalDateTime from = to.minusMonths(1); // from : 지난 달 1일 00:00

        // from ~ to 시간에 insert 된 candleDay 데이터들을 리스트로 조회
        List<CandleDay> candles = candleDayRepository.findByTokenIdAndCandleTimeBetween(tokenId, from, to);

        if (candles.isEmpty()) return null;

        // candleDay 리스트 중 가장 큰 값, 작은 값 조회
        double high = candles.stream().mapToDouble(CandleDay::getHighPrice).max().getAsDouble();
        double low  = candles.stream().mapToDouble(CandleDay::getLowPrice).min().getAsDouble();

        return CandleMonth.builder()
                .tokenId(tokenId)
                .highPrice(high)
                .lowPrice(low)
                .candleTime(from)
                .build();
    }
}

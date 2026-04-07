package server.batch.candle.job.candleDay;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleDay;
import server.batch.candle.entity.CandleHour;
import server.batch.candle.repository.CandleHourRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CandleDayProcessor implements ItemProcessor<Long, CandleDay> {

    private final CandleHourRepository candleHourRepository;

    // ItemReader 에서 from ~ to 까지 (ex : 2시부터 3시까지) 거래된 토큰 id 리스트 속 tokenId 가 하나씩 넘어온다.
    @Override
    public CandleDay process(Long tokenId) throws Exception {
        // candleMinute 의 고가, 저가들 중 가장 큰 값, 가장 작은 값을 찾아서 반환

        LocalDateTime to = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS);
        LocalDateTime from = to.minusDays(1);

        // from ~ to 시간에 insert 된 candleMinute 데이터들을 리스트로 조회
        List<CandleHour> candles = candleHourRepository.findByTokenIdAndCandleTimeBetween(tokenId, from, to);

        if (candles.isEmpty()) return null;

        // candleMinute 리스트 중 가장 큰 값, 작은 값 조회
        double high = candles.stream().mapToDouble(CandleHour::getHighPrice).max().getAsDouble();
        double low  = candles.stream().mapToDouble(CandleHour::getLowPrice).min().getAsDouble();

        return CandleDay.builder()
                .tokenId(tokenId)
                .highPrice(high)
                .lowPrice(low)
                .candleTime(from)
                .build();
    }
}

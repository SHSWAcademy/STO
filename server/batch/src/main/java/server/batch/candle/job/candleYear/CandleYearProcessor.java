package server.batch.candle.job.candleYear;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleMonth;
import server.batch.candle.entity.CandleYear;
import server.batch.candle.repository.CandleMonthRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CandleYearProcessor implements ItemProcessor<Long, CandleYear> {

    private final CandleMonthRepository candleMonthRepository;

    // ItemReader 에서 from ~ to 까지 거래된 토큰 id 리스트 속 tokenId 가 하나씩 넘어온다.
    @Override
    public CandleYear process(Long tokenId) throws Exception {

        LocalDateTime to = LocalDateTime.now().withDayOfYear(1).truncatedTo(ChronoUnit.DAYS); // to : 올해 1월 1일 00:00
        LocalDateTime from = to.minusYears(1); // from : 작년 1월 1일 00:00

        // from ~ to 시간에 insert 된 candleMonth 데이터들을 리스트로 조회
        List<CandleMonth> candles = candleMonthRepository.findByTokenIdAndCandleTimeBetween(tokenId, from, to);

        if (candles.isEmpty()) return null;

        // candleMonth 리스트 중 가장 큰 값, 작은 값 조회
        double high = candles.stream().mapToDouble(CandleMonth::getHighPrice).max().getAsDouble();
        double low  = candles.stream().mapToDouble(CandleMonth::getLowPrice).min().getAsDouble();

        return CandleYear.builder()
                .tokenId(tokenId)
                .highPrice(high)
                .lowPrice(low)
                .candleTime(from)
                .build();
    }
}

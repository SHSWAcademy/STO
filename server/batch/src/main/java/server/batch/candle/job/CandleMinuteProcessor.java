package server.batch.candle.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleMinute;
import server.batch.token.entity.Token;
import server.batch.trade.entity.Trade;
import server.batch.trade.repository.TradeRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleMinuteProcessor implements ItemProcessor<Token, CandleMinute> { // 들어오는 데이터 : Token, 나가는 데이터 : CandleMinute

    private final TradeRepository tradeRepository;

    @Override
    public CandleMinute process(Token token) throws Exception {
        // 배치가 오후 2시 4분 52초에 시작했을 경우 : truncatedTo로 52초를 버린다 => to : 4분 00초, from : to 에서 1분 지난 시간까지
        LocalDateTime to = LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES);
        LocalDateTime from = to.minusMinutes(1);

        List<Trade> trades = tradeRepository.findByTokenIdAndExecutedAtBetween(token.getTokenId(), from, to);

        if (trades.isEmpty()) return null; // 거래가 없으면 null 반환

        // list 에서 가장 큰 값, 가장 작은 값 찾기
        double highPrice = trades.stream().mapToDouble(Trade::getTradePrice).max().getAsDouble();
        double lowPrice  = trades.stream().mapToDouble(Trade::getTradePrice).min().getAsDouble();

        return CandleMinute.builder()
                .tokenId(token.getTokenId())
                .highPrice(highPrice)
                .lowPrice(lowPrice)
                .candleTime(from)
                .build();
    }
}

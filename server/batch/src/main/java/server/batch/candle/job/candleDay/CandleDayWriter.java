package server.batch.candle.job.candleDay;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleDay;
import server.batch.candle.repository.CandleDayRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleDayWriter implements ItemWriter<CandleDay> {
    private final CandleDayRepository candleDayRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    @Override
    public void write(Chunk<? extends CandleDay> chunk) throws Exception {
        // 프로세스에서 CandleHour 값들을 chunk 로 받아 DB에 저장
        candleDayRepository.saveAll(chunk.getItems());

        // Redis Publish -> main 으로 전달
        for (CandleDay candleDay : chunk.getItems()) {
            String channel = "candle:" + candleDay.getTokenId() + ":DAY";
            String payload = objectMapper.writeValueAsString(candleDay);
            redisTemplate.convertAndSend(channel, payload);
            log.info("Published candle : {}", channel);
        }
    }
}

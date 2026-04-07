package server.batch.candle.job.candleYear;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleYear;
import server.batch.candle.repository.CandleYearRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleYearWriter implements ItemWriter<CandleYear> {
    private final CandleYearRepository candleYearRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void write(Chunk<? extends CandleYear> chunk) throws Exception {
        // 프로세스에서 CandleYear 값들을 chunk 로 받아 DB에 저장
        candleYearRepository.saveAll(chunk.getItems());

        // Redis Publish -> main 으로 전달
        for (CandleYear candleYear : chunk.getItems()) {
            String channel = "candle:" + candleYear.getTokenId() + ":YEAR";
            String payload = objectMapper.writeValueAsString(candleYear);
            redisTemplate.convertAndSend(channel, payload);
            log.info("Published candle : {}", channel);
        }
    }
}

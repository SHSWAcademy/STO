package server.batch.candle.job.candleMonth;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleMonth;
import server.batch.candle.repository.CandleMonthRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleMonthWriter implements ItemWriter<CandleMonth> {
    private final CandleMonthRepository candleMonthRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void write(Chunk<? extends CandleMonth> chunk) throws Exception {
        // 프로세스에서 CandleMonth 값들을 chunk 로 받아 DB에 저장
        candleMonthRepository.saveAll(chunk.getItems());

        // Redis Publish -> main 으로 전달
        for (CandleMonth candleMonth : chunk.getItems()) {
            String channel = "candle:" + candleMonth.getTokenId() + ":MONTH";
            String payload = objectMapper.writeValueAsString(candleMonth);
            redisTemplate.convertAndSend(channel, payload);
            log.info("Published candle : {}", channel);
        }
    }
}

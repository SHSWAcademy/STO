package server.batch.candle.job.candleHour;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleHour;
import server.batch.candle.repository.CandleHourRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleHourWriter implements ItemWriter<CandleHour> {
    private final CandleHourRepository candleHourRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;
    @Override
    public void write(Chunk<? extends CandleHour> chunk) throws Exception {
        // 프로세스에서 CandleHour 값들을 chunk 로 받아 DB에 저장
        candleHourRepository.saveAll(chunk.getItems());


    }
}

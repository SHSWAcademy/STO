package server.batch.candle.job.candleMinute;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import server.batch.candle.entity.CandleMinute;
import com.fasterxml.jackson.databind.ObjectMapper;
import server.batch.candle.repository.CandleMinuteRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class CandleMinuteWriter implements ItemWriter<CandleMinute> { // 쓰는 데이터 : CandleMinute
    // reader -> processor -> writer 흐름

    private final CandleMinuteRepository candleMinuteRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void write(Chunk<? extends CandleMinute> chunk) throws Exception {
        candleMinuteRepository.saveAll(chunk.getItems()); // 벌크 insert : 하나의 쿼리로 전부 insert

    }
}

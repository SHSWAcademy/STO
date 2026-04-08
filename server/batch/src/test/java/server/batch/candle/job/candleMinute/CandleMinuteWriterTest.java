package server.batch.candle.job.candleMinute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import server.batch.candle.entity.CandleMinute;
import server.batch.candle.repository.CandleMinuteRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CandleMinuteWriterTest {

    @Mock
    private CandleMinuteRepository candleMinuteRepository;

    @InjectMocks
    private CandleMinuteWriter writer;

    @Test
    void saveAll_벌크_insert_호출_검증() throws Exception {
        List<CandleMinute> items = List.of(
                CandleMinute.builder()
                        .tokenId(1L)
                        .highPrice(300.0)
                        .lowPrice(100.0)
                        .candleTime(LocalDateTime.of(2024, 1, 1, 14, 3, 0))
                        .build(),
                CandleMinute.builder()
                        .tokenId(2L)
                        .highPrice(500.0)
                        .lowPrice(200.0)
                        .candleTime(LocalDateTime.of(2024, 1, 1, 14, 3, 0))
                        .build()
        );

        Chunk<CandleMinute> chunk = new Chunk<>(items);

        writer.write(chunk);

        verify(candleMinuteRepository).saveAll(items);
    }

    @Test
    void 빈_chunk_전달시_saveAll_빈_리스트로_호출() throws Exception {
        Chunk<CandleMinute> chunk = new Chunk<>(List.of());

        writer.write(chunk);

        verify(candleMinuteRepository).saveAll(List.of());
    }
}

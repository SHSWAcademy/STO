package server.batch.candle.job.candleMinute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import server.batch.candle.entity.CandleMinute;
import server.batch.token.entity.Token;
import server.batch.trade.entity.Trade;
import server.batch.trade.repository.TradeRepository;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandleMinuteProcessorTest {

    @Mock
    private TradeRepository tradeRepository;

    @InjectMocks
    private CandleMinuteProcessor processor;

    // 배치가 14시 4분 52초에 실행됐다고 가정
    private static final LocalDateTime FIXED_NOW = LocalDateTime.of(2024, 1, 1, 14, 4, 52);
    // truncatedTo(MINUTES) 결과: 14:04:00
    private static final LocalDateTime EXPECTED_TO   = LocalDateTime.of(2024, 1, 1, 14, 4, 0);
    // to - 1분: 14:03:00
    private static final LocalDateTime EXPECTED_FROM = LocalDateTime.of(2024, 1, 1, 14, 3, 0);

    private Token token;

    @BeforeEach
    void setUp() {
        // Token 은 @NoArgsConstructor 만 있어서 ReflectionTestUtils 로 필드 세팅
        token = new Token();
        ReflectionTestUtils.setField(token, "tokenId", 1L);
    }

    @Test
    void 해당_구간에_거래가_없으면_null_반환() throws Exception {
        try (MockedStatic<LocalDateTime> mockedLdt = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedLdt.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            when(tradeRepository.findByTokenIdAndExecutedAtBetween(1L, EXPECTED_FROM, EXPECTED_TO))
                    .thenReturn(List.of());

            CandleMinute result = processor.process(token);

            assertThat(result).isNull();
        }
    }

    @Test
    void 여러_거래에서_고가_저가_올바르게_계산() throws Exception {
        try (MockedStatic<LocalDateTime> mockedLdt = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedLdt.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            List<Trade> trades = List.of(
                    Trade.builder().tradePrice(100L).build(),
                    Trade.builder().tradePrice(300L).build(),
                    Trade.builder().tradePrice(200L).build()
            );

            when(tradeRepository.findByTokenIdAndExecutedAtBetween(1L, EXPECTED_FROM, EXPECTED_TO))
                    .thenReturn(trades);

            CandleMinute result = processor.process(token);

            assertThat(result).isNotNull();
            assertThat(result.getHighPrice()).isEqualTo(300.0);
            assertThat(result.getLowPrice()).isEqualTo(100.0);
            assertThat(result.getTokenId()).isEqualTo(1L);
            assertThat(result.getCandleTime()).isEqualTo(EXPECTED_FROM);
        }
    }

    @Test
    void 단일_거래시_고가_저가가_동일() throws Exception {
        try (MockedStatic<LocalDateTime> mockedLdt = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedLdt.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            List<Trade> trades = List.of(
                    Trade.builder().tradePrice(500L).build()
            );

            when(tradeRepository.findByTokenIdAndExecutedAtBetween(1L, EXPECTED_FROM, EXPECTED_TO))
                    .thenReturn(trades);

            CandleMinute result = processor.process(token);

            assertThat(result).isNotNull();
            assertThat(result.getHighPrice()).isEqualTo(result.getLowPrice());
            assertThat(result.getHighPrice()).isEqualTo(500.0);
        }
    }

    @Test
    void candleTime이_from_시간으로_설정됨() throws Exception {
        try (MockedStatic<LocalDateTime> mockedLdt = mockStatic(LocalDateTime.class, CALLS_REAL_METHODS)) {
            mockedLdt.when(LocalDateTime::now).thenReturn(FIXED_NOW);

            List<Trade> trades = List.of(
                    Trade.builder().tradePrice(100L).build()
            );

            when(tradeRepository.findByTokenIdAndExecutedAtBetween(1L, EXPECTED_FROM, EXPECTED_TO))
                    .thenReturn(trades);

            CandleMinute result = processor.process(token);

            // candleTime 은 구간 시작 시간(from) 이어야 한다
            assertThat(result.getCandleTime()).isEqualTo(EXPECTED_FROM);
        }
    }
}

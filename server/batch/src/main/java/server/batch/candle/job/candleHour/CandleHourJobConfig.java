package server.batch.candle.job.candleHour;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import server.batch.candle.entity.CandleHour;
import server.batch.candle.repository.CandleMinuteRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CandleHourJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final CandleMinuteRepository candleMinuteRepository;
    private final CandleHourProcessor processor;
    private final CandleHourWriter writer;

    @Bean
    public Job candleHourJob() {
        return new JobBuilder("candleHourJob", jobRepository)
                .start(candleHourStep())
                .build();
    }

    @Bean
    public Step candleHourStep() {
        return new StepBuilder("candleHourStep", jobRepository)
                // tokenId가 들어오고 CandleHour로 나간다
                .<Long, CandleHour>chunk(50, txManager)
                .reader(candleHourReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ListItemReader<Long> candleHourReader() {
        // 배치가 시작될 때 현재 시간이 3시 20분이라면 20분을 깎아 3시만 남긴다
        LocalDateTime to = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS); // to : 3시
        LocalDateTime from = to.minusHours(1); // from : 2시
        // 2시부터 3시까지 거래된 토큰 id 들을 찾아 리스트 반환
        List<Long> tokenIds = candleMinuteRepository.findDistinctTokenIdByCandleTimeBetween(from, to);
        return new ListItemReader<>(tokenIds);
    }
}

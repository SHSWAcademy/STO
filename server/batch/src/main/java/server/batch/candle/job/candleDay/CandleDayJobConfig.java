package server.batch.candle.job.candleDay;

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
import server.batch.candle.entity.CandleDay;
import server.batch.candle.repository.CandleHourRepository;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CandleDayJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final CandleHourRepository candleHourRepository;
    private final CandleDayProcessor processor;
    private final CandleDayWriter writer;

    @Bean
    public Job candleDayJob() {
        return new JobBuilder("candleDayJob", jobRepository)
                .start(candleDayStep())
                .build();
    }

    @Bean
    public Step candleDayStep() {
        return new StepBuilder("candleDayStep", jobRepository)
                // tokenId가 들어오고 CandleHour로 나간다
                .<Long, CandleDay>chunk(50, txManager)
                .reader(candleDayReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ListItemReader<Long> candleDayReader() {
        // 배치가 시작될 때 현재 시간이 3시 20분이라면 20분을 깎아 3시만 남긴다
        LocalDateTime to = LocalDateTime.now().truncatedTo(ChronoUnit.DAYS); // to : 오늘 00:00
        LocalDateTime from = to.minusDays(1); // from : 어제 00:00
        // 어제 00:00 ~ 오늘 00:00 사이 거래된 토큰 id 들을 찾아 리스트 반환
        List<Long> tokenIds = candleHourRepository.findDistinctTokenIdByCandleTimeBetween(from, to);
        return new ListItemReader<>(tokenIds);
    }
}

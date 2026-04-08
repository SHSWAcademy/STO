package server.batch.candle.job.candleYear;

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
import server.batch.candle.entity.CandleYear;
import server.batch.candle.repository.CandleMonthRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CandleYearJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final CandleMonthRepository candleMonthRepository;
    private final CandleYearProcessor processor;
    private final CandleYearWriter writer;

    @Bean
    public Job candleYearJob() {
        return new JobBuilder("candleYearJob", jobRepository)
                .start(candleYearStep())
                .build();
    }

    @Bean
    public Step candleYearStep() {
        return new StepBuilder("candleYearStep", jobRepository)
                // tokenId가 들어오고 CandleYear로 나간다
                .<Long, CandleYear>chunk(50, txManager)
                .reader(candleYearReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ListItemReader<Long> candleYearReader() {
        LocalDateTime to = LocalDateTime.now().withDayOfYear(1).truncatedTo(ChronoUnit.DAYS); // to : 올해 1월 1일 00:00
        LocalDateTime from = to.minusYears(1); // from : 작년 1월 1일 00:00
        // 작년 1월 1일 00:00 ~ 올해 1월 1일 00:00 사이 거래된 토큰 id 들을 찾아 리스트 반환
        List<Long> tokenIds = candleMonthRepository.findDistinctTokenIdByCandleTimeBetween(from, to);
        return new ListItemReader<>(tokenIds);
    }
}

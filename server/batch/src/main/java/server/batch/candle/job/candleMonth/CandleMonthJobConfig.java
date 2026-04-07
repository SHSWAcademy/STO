package server.batch.candle.job.candleMonth;

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
import server.batch.candle.entity.CandleMonth;
import server.batch.candle.repository.CandleDayRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CandleMonthJobConfig {
    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final CandleDayRepository candleDayRepository;
    private final CandleMonthProcessor processor;
    private final CandleMonthWriter writer;

    @Bean
    public Job candleMonthJob() {
        return new JobBuilder("candleMonthJob", jobRepository)
                .start(candleMonthStep())
                .build();
    }

    @Bean
    public Step candleMonthStep() {
        return new StepBuilder("candleMonthStep", jobRepository)
                // tokenId가 들어오고 CandleMonth로 나간다
                .<Long, CandleMonth>chunk(50, txManager)
                .reader(candleMonthReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ListItemReader<Long> candleMonthReader() {
        LocalDateTime to = LocalDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS); // to : 이번 달 1일 00:00
        LocalDateTime from = to.minusMonths(1); // from : 지난 달 1일 00:00
        // 지난 달 1일 00:00 ~ 이번 달 1일 00:00 사이 거래된 토큰 id 들을 찾아 리스트 반환
        List<Long> tokenIds = candleDayRepository.findDistinctTokenIdByCandleTimeBetween(from, to);
        return new ListItemReader<>(tokenIds);
    }
}

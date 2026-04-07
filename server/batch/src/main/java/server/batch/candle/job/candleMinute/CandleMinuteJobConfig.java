package server.batch.candle.job.candleMinute;

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
import server.batch.candle.entity.CandleMinute;
import server.batch.token.entity.Token;
import server.batch.token.entity.TokenStatus;
import server.batch.token.repository.TokenRepository;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class CandleMinuteJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final TokenRepository tokenRepository;
    private final CandleMinuteProcessor processor;
    private final CandleMinuteWriter writer;

    @Bean
    public Job candleMinuteJob() {
        return new JobBuilder("candleMinuteJob", jobRepository)
                .start(candleMinuteStep())
                .build();
    }

    @Bean
    public Step candleMinuteStep() {
        return new StepBuilder("candleMinuteStep", jobRepository)
                .<Token, CandleMinute>chunk(50, txManager)
                .reader(candleMinuteReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public ListItemReader<Token> candleMinuteReader() {
        List<Token> tokens = tokenRepository.findAllByTokenStatus(TokenStatus.TRADING);
        return new ListItemReader<>(tokens);
    }
}

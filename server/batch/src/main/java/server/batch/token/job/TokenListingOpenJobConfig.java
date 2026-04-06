package server.batch.token.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class TokenListingOpenJobConfig {

    // NOTE:
    // This config only wires the batch Job and Step together.
    // The actual business logic stays in Tasklet -> Service.
    private final TokenListingOpenTasklet tokenListingOpenTasklet;

    @Bean
    public Job tokenListingOpenJob(
            JobRepository jobRepository,
            Step tokenListingOpenStep
    ) {
        return new JobBuilder("tokenListingOpenJob", jobRepository)
                .start(tokenListingOpenStep)
                .build();
    }

    @Bean
    public Step tokenListingOpenStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager
    ) {
        return new StepBuilder("tokenListingOpenStep", jobRepository)
                .tasklet(tokenListingOpenTasklet, transactionManager)
                .build();
    }
}

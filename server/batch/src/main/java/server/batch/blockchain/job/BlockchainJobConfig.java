package server.batch.blockchain.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import server.batch.blockchain.job.tasklet.BlockchainTasklet;

@Configuration
@RequiredArgsConstructor
public class BlockchainJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final BlockchainTasklet blockchainTasklet;

    @Bean
    public Job blockchainJob() {
        return new JobBuilder("blockchainJob", jobRepository)
                .start(blockchainStep())
                .build();
    }

    @Bean
    public Step blockchainStep() {
        return new StepBuilder("blockchainStep", jobRepository)
                .tasklet(blockchainTasklet, txManager)
                .build();
    }
}

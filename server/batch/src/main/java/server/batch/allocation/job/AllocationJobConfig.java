package server.batch.allocation.job;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import server.batch.allocation.dto.AllocationResult;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.processor.AllocationProcessor;
import server.batch.allocation.reader.AllocationEventReader;
import server.batch.allocation.repository.AllocationEventRepository;
import server.batch.allocation.writer.AllocationWriter;

@Configuration
@RequiredArgsConstructor
public class AllocationJobConfig {

    private final JobRepository jobRepository;
    private final PlatformTransactionManager txManager;
    private final AllocationEventRepository allocationEventRepository;
    private final AllocationProcessor processor;
    private final AllocationWriter writer;

    @Bean
    public Job allocationJob() {
        return new JobBuilder("allocationJob", jobRepository)
                .start(allocationStep())
                .build();
    }

    @Bean
    public Step allocationStep() {
        return new StepBuilder("allocationStep", jobRepository)
                .<AllocationEvent, AllocationResult>chunk(1, txManager)    // 이벤트 수는 무방함 (1로해도)
                .reader(allocationEventReader())
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    @StepScope
    public AllocationEventReader allocationEventReader() {
        return new AllocationEventReader(allocationEventRepository);
    }
}

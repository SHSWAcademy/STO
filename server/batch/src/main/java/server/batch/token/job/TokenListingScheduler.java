package server.batch.token.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenListingScheduler {

    private final JobLauncher jobLauncher;
    
    // NOTE:
    // Inject the job by its bean name so the scheduler still points to
    // the correct job even after other batch jobs are added later.
    @Qualifier("tokenListingOpenJob")
    private final Job tokenListingOpenJob;

    @Scheduled(cron = "0 0 9 * * *")
    public void runTokenListingOpenJob() throws Exception {
        JobParameters jobParameters = new JobParametersBuilder()
                .addLong("currentTime", System.currentTimeMillis())
                .toJobParameters();

        log.info("Starting tokenListingOpenJob");
        jobLauncher.run(tokenListingOpenJob, jobParameters);
    }
}

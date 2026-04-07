package server.batch.candle.job;

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
public class CandleScheduler {

    private final JobLauncher jobLauncher;

    @Qualifier("candleMinuteJob")
    private final Job candleMinuteJob;

    @Scheduled(cron = "0 * * * * *")  // 매 분 0초
    public void runCandleMinuteJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("currentTime", System.currentTimeMillis())
                .toJobParameters();

        log.info("Starting candleMinuteJob");
        jobLauncher.run(candleMinuteJob, params);
    }
}
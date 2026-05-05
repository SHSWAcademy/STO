package server.batch.blockchain.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainBatchScheduler {

    private final JobLauncher jobLauncher;
    private final JobExplorer jobExplorer;

    @Qualifier("blockchainJob")
    private final Job blockchainJob;


    @Scheduled(cron = "0/10 * * * * *") // 매 10초
    public void runBlockchainJob() throws Exception {

        int runningCount = jobExplorer
                .findRunningJobExecutions("blockchainJob")
                .size();
        if (runningCount > 0) {
            log.info("블록체인 배치 이미 실행 중 - 스킵");
            return;
        }
        JobParameters params = new JobParametersBuilder()
                .addLong("currentTime", System.currentTimeMillis())
                .toJobParameters();

        log.info("블록체인 배치 작업 시작");
        jobLauncher.run(blockchainJob, params);
    }
}

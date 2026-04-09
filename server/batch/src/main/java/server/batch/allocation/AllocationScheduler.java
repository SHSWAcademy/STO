package server.batch.allocation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.batch.allocation.entity.Commons;
import server.batch.allocation.repository.CommonsRepository;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@Slf4j
public class AllocationScheduler {

    private final JobLauncher jobLauncher;
    private final CommonsRepository commonsRepository;

    @Qualifier("allocationJob")
    private final Job allocationJob;

    @Scheduled(cron = "0 0 0 * * *")
    public void runAllocationJob() throws Exception {
        // 시스템 기초설정조회
        Commons commons = commonsRepository.findFirstBy();
        // 배일일 아니면 종료
        if (LocalDate.now().getDayOfMonth() != commons.getAllocateDate()) {
            return;
        }
        // 작업 시작
        JobParameters params = new JobParametersBuilder()
                .addLong("currentTime", System.currentTimeMillis())
                .toJobParameters();

        log.info("배당 지급 배치 작업 시작");
        jobLauncher.run(allocationJob, params);
    }
}

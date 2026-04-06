package server.batch.token.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import server.batch.token.service.TokenListingService;

@Component
@RequiredArgsConstructor
@Slf4j
public class TokenListingOpenTasklet implements Tasklet {

    private final TokenListingService tokenListingService;

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int updatedCount = tokenListingService.openIssuedTokens();
        log.info("Token listing batch finished. Updated count: {}", updatedCount);
        return RepeatStatus.FINISHED;
    }
}

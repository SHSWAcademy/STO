package server.main.token.scheduler;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.main.global.util.GeminiClient;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;
import server.main.trade.repository.TradeRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiSummaryScheduler {
    private final TokenRepository tokenRepository;
    private final TradeRepository tradeRepository;
    private final GeminiClient geminiClient;

//    @PostConstruct
//    public void init() {
//        updateAllAiSummaries();
//    }

    @Scheduled(cron = "0 0 0/3 * * *")
    public void updateAllAiSummaries() {
        List<Token> tokens = tokenRepository.findAllTokensWithAsset();

        for (Token token : tokens) {
            try {
                List<Object[]> weeklyStats = tradeRepository.findWeeklyTradeStats(
                        token.getTokenId(), LocalDateTime.now().minusWeeks(1));
                String summary = geminiClient
                        .summarizeVolumeTrend(token.getAsset().getAssetName(), weeklyStats)
                        .get();
                if (summary == null) {
                    log.warn("AI 요약 스킵 (오류 응답) - tokenId: {}", token.getTokenId());
                    continue;
                }
                token.updateAiSummary(summary);
                tokenRepository.save(token);
                log.info("AI 요약 저장 완료 - tokenId: {}", token.getTokenId());

            } catch (Exception e) {
                log.error("AI 요약 실패 - tokenId: {}, error: {}", token.getTokenId(), e.getMessage());
            }
        }
    }
}

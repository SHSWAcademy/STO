package server.main.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@RequiredArgsConstructor
@Log4j2
public class GeminiClient {

    @Value("${gemini.api.url}")
    private String apiUrl;

    @Value("${gemini.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate;

    @Async
    public CompletableFuture<String> summarizeVolumeTrend(String assetName, List<Object[]> weeklyStats) {
        String prompt = buildPrompt(assetName, weeklyStats);
        String result = callGemini(GeminiDTO.MODEL_NAME, prompt);
        if (result == null) {
            log.warn("기본 모델 한도 초과, fallback 모델로 재시도");
            result = callGemini(GeminiDTO.FALLBACK_MODEL_NAME, prompt);
        }
        return CompletableFuture.completedFuture(result);
    }

    private String callGemini(String modelName, String prompt) {
        String url = apiUrl + "/v1beta/models/" + modelName + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        GeminiDTO.Request request = new GeminiDTO.Request(prompt);
        HttpEntity<GeminiDTO.Request> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GeminiDTO.Response> response = restTemplate.postForEntity(url, entity, GeminiDTO.Response.class);
            log.info("Gemini 호출 성공 (model: {})", modelName);
            return response.getBody().getAnswer();

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            HttpStatus status = (HttpStatus) e.getStatusCode();
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                log.warn("Gemini 한도 초과 (429) - model: {}", modelName);
            } else if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                log.error("Gemini API 키 인증 실패 (401/403) - model: {}", modelName);
            } else {
                log.error("Gemini 클라이언트 오류 ({}) - model: {}", status, modelName);
            }
            return null;
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            log.error("Gemini 서버 오류 ({}) - model: {}", e.getStatusCode(), modelName);
            return null;
        } catch (Exception e) {
            log.error("알 수 없는 오류 - model: {}, error: {}", modelName, e.getMessage());
            return null;
        }
    }

    // 프롬포트
    private String buildPrompt(String assetName, List<Object[]> weeklyStats) {
        StringBuilder sb = new StringBuilder();
        sb.append("너는 STO(토큰증권) 데이터를 객관적으로 요약하는 데이터 분석가야.\n\n");

        // 7일 트레이드 데이터
        sb.append("### [").append(assetName).append("] 최근 7일 통계 ###\n");
        for (Object[] row : weeklyStats) {
            sb.append("- ").append(row[0]).append(": 거래량 ").append(row[1])
                    .append(", 평균가 ").append(row[3]).append("원\n");
        }
        // 출력 규칙
        sb.append("\n### [출력 규칙 - 반드시 준수] ###\n");
        sb.append("1. **절대 '투자 권유'나 '예측'을 하지 마.** (검열 방지)\n");
        sb.append("2. 데이터에 나타난 현상(거래량 증감, 가격 추이)만 **한 줄의 완성된 문장**으로 요약해.\n");
        sb.append("3. 문장 끝은 반드시 마침표(.)로 끝나야 하며, 중간에 생략하지 마.\n");
        sb.append("4. **'급등', '폭등', '강력 추천' 같은 단어는 절대 사용하지 마.** 대신 '상승세', '유입 증가', '변동성 확대' 등의 표현을 써.\n");
        sb.append("5. 답변 시작에 '분석 결과:' 같은 서두 떼고 바로 본론만 말해.\n\n");
        sb.append("6. 마지막으로 문장은 무조건 끝까지 중간에 끊기지 말것.\n");
        sb.append("요약 (마침표까지 완결된 한 문장):");
        return sb.toString();
    }
}

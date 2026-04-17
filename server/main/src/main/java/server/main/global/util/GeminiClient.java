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
        String url = apiUrl + "/v1beta/models/" + GeminiDTO.MODEL_NAME + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        GeminiDTO.Request request = new GeminiDTO.Request(buildPrompt(assetName, weeklyStats));
        HttpEntity<GeminiDTO.Request> entity = new HttpEntity<>(request, headers);

        try {
            ResponseEntity<GeminiDTO.Response> response = restTemplate.postForEntity(url, entity, GeminiDTO.Response.class);
            log.info("제미나이 호출 결과 성공");
            return CompletableFuture.completedFuture(response.getBody().getAnswer());

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            HttpStatus status = (HttpStatus) e.getStatusCode();
            if (status == HttpStatus.TOO_MANY_REQUESTS) {
                // 사용량 오류
                log.warn("Gemini API 한도 초과 (429): {}", e.getMessage());
                return CompletableFuture.completedFuture("현재 분석 요청이 너무 많습니다. 1분 후 다시 시도해 주세요.");
            }
            else if (status == HttpStatus.UNAUTHORIZED || status == HttpStatus.FORBIDDEN) {
                // 키 오류
                log.error("Gemini API 키 인증 실패 (401/403): API 키를 확인하세요.");
                return CompletableFuture.completedFuture("서비스 점검 중입니다. 이용에 불편을 드려 죄송합니다.");
            }
            else {
                // 우리서버 오류
                log.error("Gemini API 클라이언트 오류 ({}): {}", status, e.getMessage());
                return CompletableFuture.completedFuture("데이터 분석 중 오류가 발생했습니다.");
            }
        } catch (org.springframework.web.client.HttpServerErrorException e) {
            // 제미나이 서버 오류
            log.error("Gemini 서버 오류 ({}): {}", e.getStatusCode(), e.getMessage());
            return CompletableFuture.completedFuture("AI 서버가 일시적으로 응답하지 않습니다. 잠시 후 다시 시도해 주세요.");
        } catch (Exception e) {
            // 알수없음
            log.error("알 수 없는 오류 발생: {}", e.getMessage());
            return CompletableFuture.completedFuture("예측하지 못한 오류가 발생했습니다.");
        }
    }

    // 프롬포트
    private String buildPrompt(String assetName, List<Object[]> weeklyStats) {
        StringBuilder sb = new StringBuilder();

        sb.append("너는 STO(토큰증권) 시장 데이터 분석 전문가야. 아래 데이터를 보고 투자자가 참고할 수 있도록 전문적인 인사이트를 제공해줘.\n\n");
        sb.append("### [").append(assetName).append("] 종목 최근 7일 거래 데이터 ###\n");
        for (Object[] row : weeklyStats) {
            sb.append("- 날짜: ").append(row[0])
                    .append(" | 거래량: ").append(row[1])
                    .append(" | 총금액: ").append(row[2])
                    .append(" | 평균가: ").append(row[3])
                    .append("\n");
        }
        sb.append("\n### 분석 지시사항 ###\n");
        sb.append("- 분석 내용은 반드시 하나의 완성된 문장으로 마침표(.)까지 찍어서 출력해.\n");
        sb.append("- '추세 분석이 어렵다'는 말은 하지 말고, 현재 수치에서 보이는 특징을 전문적으로 설명해.\n");
        sb.append("- 문장 중간에 끊기지 않도록 끝까지 서술해.");
        sb.append("1. 위 데이터를 바탕으로 전체적인 거래 추세(상승/하락/횡보)를 분석할 것.\n");
        sb.append("2. 거래량 급증이나 가격 변동의 특이사항이 있다면 언급할 것.\n");
        sb.append("3. 마지막에 투자자가 유의해야 할 점을 포함하여 '한 줄의 완성된 문장'으로 요약할 것.\n");
        sb.append("4. 답변은 한국어 기준 100자~150자 내외로 전문성 있게 작성할 것.\n");
        sb.append("5. 데이터가 충분하지 않더라도 현재 수치를 바탕으로 시장의 분위기를 최대한 추론하여 답변할 것.\n");
        sb.append("6. '분석이 어렵다'는 말은 생략하고, 현재 공개된 정보를 바탕으로 즉각적인 통찰을 제공할 것.\n");
        sb.append("\n요약 결과:");
        return sb.toString();
    }
}

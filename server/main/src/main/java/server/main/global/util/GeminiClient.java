package server.main.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

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
    public CompletableFuture<String> summarizeVolumeTrend(String prompt) {
        String url = apiUrl + "/v1beta/models/" + GeminiDTO.MODEL_NAME + ":generateContent?key=" + apiKey;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        GeminiDTO.Request request = new GeminiDTO.Request(prompt);
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
}

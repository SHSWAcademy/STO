package server.main.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import server.main.token.dto.GeminiDTO;

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
            log.info("제미나이 호출 결과 {} ", response.getBody().getAnswer());
            return CompletableFuture.completedFuture(response.getBody().getAnswer());
        } catch (Exception e) {
            return CompletableFuture.completedFuture("요약 데이터를 가져오는 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}

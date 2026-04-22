package server.main.global.util.news;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@RequiredArgsConstructor
@Log4j2
public class NaverNewsClient {

    @Value("${naver.api.news-url}")
    private String newsUrl;

    @Value("${naver.api.client-id}")
    private String clientId;

    @Value("${naver.api.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate;

    public List<NaverNewsResponseDto.Item> fetchStoNews(int display) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", clientId);
        headers.set("X-Naver-Client-Secret", clientSecret);

        String url = newsUrl + "?query=STO+토큰증권&display=" + display + "&sort=date";
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<NaverNewsResponseDto> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, NaverNewsResponseDto.class);
            List<NaverNewsResponseDto.Item> items = response.getBody().getItems();
            return items != null ? items : List.of();
        } catch (Exception e) {
            log.error("네이버 뉴스 API 호출 실패: {}", e.getMessage());
            return List.of();
        }
    }
}

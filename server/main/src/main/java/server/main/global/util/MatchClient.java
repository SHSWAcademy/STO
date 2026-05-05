package server.main.global.util;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;

// main -> match 전달
@Component
public class MatchClient {

    @Value("${match.server.url}")
    private String matchServerUrl;

    private final RestTemplate restTemplate;

    public MatchClient(@Qualifier("matchRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public String getOrderBookSnapshot(Long tokenId) {
        String url = matchServerUrl + "/internal/orders/" + tokenId;
        String snapshot = restTemplate.getForObject(url, String.class);
        if (!StringUtils.hasText(snapshot)) {
            throw new RestClientException("match server returned empty orderbook snapshot. tokenId=" + tokenId);
        }
        return snapshot;
    }

    public MatchResultDto sendOrder(MatchOrderRequestDto dto) {
        MatchResultDto body = restTemplate.postForObject(matchServerUrl + "/internal/orders", dto, MatchResultDto.class);
        if (body == null) {
            throw new RestClientException("match server response body was null. orderId=" + dto.getOrderId());
        }
        return body;
    }

    public MatchResultDto updateOrder(UpdateMatchOrderRequestDto dto) {
        String url = matchServerUrl + "/internal/orders/" + dto.getOrderId();
        MatchResultDto body = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(dto), MatchResultDto.class
        ).getBody();
        if (body == null) {
            throw new RestClientException("match server response body was null. orderId=" + dto.getOrderId());
        }
        return body;
    }

    public void cancelOrder(Long orderId, Long tokenId) {
        String url = matchServerUrl + "/internal/orders/" + orderId + "?tokenId=" + tokenId;
        restTemplate.delete(url);
    }
}

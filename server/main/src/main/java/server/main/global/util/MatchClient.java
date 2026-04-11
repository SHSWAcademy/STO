package server.main.global.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import lombok.RequiredArgsConstructor;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;


// main -> match 로 전달
@Component
@RequiredArgsConstructor
public class MatchClient {

    @Value("${match.server.url}")
    private String matchServerUrl;

    private final RestTemplate restTemplate;

    // 상세 페이지로 들어올 때 호가창 정보를 먼저 매치 서버에서 받아와야 한다 (현재까지의 호가 상태 스냅샷 받아오기)
    // 웹소켓이 있어도 해당 로직이 없으면 상세 페이지 접속 후 첫 호가가 일어나기 전까지 호가 상태를 조회할 수 없다
    public String getOrderBookSnapshot(Long tokenId) {
        String url = matchServerUrl + "/internal/orderBook/" + tokenId;
        return restTemplate.getForObject(url, String.class);
    }

    // 주문 생성
    public MatchResultDto sendOrder(MatchOrderRequestDto dto) {
        return restTemplate.postForObject(matchServerUrl + "/internal/orders", dto, MatchResultDto.class);
    }

    // 주문 수정 — 수정 후 재매칭 결과를 받아야 하므로 exchange() 사용 (put()은 응답 body를 버림)
    public MatchResultDto updateOrder(UpdateMatchOrderRequestDto dto) {
        String url = matchServerUrl + "/internal/orders/" + dto.getOrderId();
        MatchResultDto body = restTemplate.exchange(
                url, HttpMethod.PUT, new HttpEntity<>(dto), MatchResultDto.class
        ).getBody();
        if (body == null) {
            throw new org.springframework.web.client.RestClientException("match 서버 응답 body가 null입니다. orderId=" + dto.getOrderId());
        }
        return body;
    }

    // 주문 취소 — match가 어느 오더북에서 찾을지 알 수 있도록 tokenId를 쿼리 파라미터로 전달
    public void cancelOrder(Long orderId, Long tokenId) {
        String url = matchServerUrl + "/internal/orders/" + orderId + "?tokenId=" + tokenId;
        restTemplate.delete(url);
    }
}

package server.main.global.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import server.main.order.dto.MatchOrderRequestDto;

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


    public void sendOrder(MatchOrderRequestDto dto) {
        restTemplate.postForObject(matchServerUrl + "/internal/orders", dto, Void.class);
    }
}

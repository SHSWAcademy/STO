package server.main.global.util;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;

import java.util.Map;


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
    public void sendOrder(MatchOrderRequestDto dto) {
        restTemplate.postForObject(matchServerUrl + "/internal/orders", dto, Void.class);
    }


    // 수정 시 match에 던지는 파라미터, orderId, sequence 만 던지는 것이 아니라 updatePrice, updateQuantity도 같이 던집니다 !!! (혹시 필요없다면 말씀해주세요)
    public void updateOrder(UpdateMatchOrderRequestDto dto) {
        String url = matchServerUrl + "/internal/orders/" + dto.getOrderId();
        restTemplate.put(url, dto);
    }

    // 주문 삭제 시 match 에게 전달, dto 말고 orderId만 던지는데 혹시 값 더 필요하다면 말씀해주세요!
    public void cancelOrder(Long orderId) {
        String url = matchServerUrl + "/internal/orders/" + orderId;
        restTemplate.delete(url);
    }
}

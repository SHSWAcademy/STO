package server.match.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderBookEventDto {
    private Long tokenId;          // 어떤 토큰의 오더북인지
    private List<PriceLevel> asks; // 매도 호가 목록 (낮은 가격 우선)
    private List<PriceLevel> bids; // 매수 호가 목록 (높은 가격 우선)

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceLevel {
        private Long price;    // 호가 가격
        private Long quantity; // 해당 가격의 총 수량 (집계값)
    }
}

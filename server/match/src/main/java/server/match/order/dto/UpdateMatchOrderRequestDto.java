package server.match.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMatchOrderRequestDto {
    private Long orderId;
    private Long tokenId;         // 어느 오더북인지 식별 (main이 전달)
    private Long updatePrice;
    private Long updateQuantity;  // 남은 수량 (main 서버가 계산해서 전달)
}

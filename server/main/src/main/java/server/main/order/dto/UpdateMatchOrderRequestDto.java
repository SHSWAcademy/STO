package server.main.order.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMatchOrderRequestDto {
    private Long orderId;        // match에 전달할 주문 ID
    private Long tokenId;        // match가 어느 오더북인지 찾기 위한 토큰 ID
    private Long updatePrice;    // 수정할 가격
    private Long updateQuantity; // 수정할 남은 수량 (filledQuantity 제외하고 계산된 값)

    // 보상 트랜잭션용 — match 서버로 전송하지 않음, Facade 내부에서만 사용
    @JsonIgnore
    private Long originalPrice;
    @JsonIgnore
    private Long originalQuantity;
}

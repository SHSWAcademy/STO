package server.main.order.dto;

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

    // 보상 트랜잭션용 — match 실패 시 원래 상태로 복구하기 위한 수정 전 값
    private Long originalPrice;
    private Long originalQuantity;
}

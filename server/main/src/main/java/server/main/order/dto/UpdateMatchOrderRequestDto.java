package server.main.order.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMatchOrderRequestDto {
    private Long orderId;           // match에 전달할 id
    private Long orderSequence;     // match에 전달할 order 순서
    private Long updatePrice;
    private Long updateQuantity;
}

package server.main.order.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequestDto {
    private Long updatePrice;
    private Long updateQuantity;
}

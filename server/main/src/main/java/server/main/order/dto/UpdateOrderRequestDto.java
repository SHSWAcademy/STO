package server.main.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateOrderRequestDto {
    @NotNull
    @Positive
    private Long updatePrice;

    @NotNull
    @Positive
    private Long updateQuantity;
}

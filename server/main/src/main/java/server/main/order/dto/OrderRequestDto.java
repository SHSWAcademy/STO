package server.main.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.main.order.entity.OrderType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderRequestDto {
    @NotNull
    @Positive
    private Long orderPrice;
    @NotNull
    @Positive
    private Long orderQuantity;
    @NotNull
    private OrderType orderType;
}

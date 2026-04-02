package server.main.order.dto;

import jakarta.validation.constraints.NotNull;
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
    private Long orderPrice;
    @NotNull
    private Long orderQuantity;
    @NotNull
    private OrderType orderType;
}

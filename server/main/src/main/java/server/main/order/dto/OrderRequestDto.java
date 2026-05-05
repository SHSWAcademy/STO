package server.main.order.dto;

import jakarta.validation.constraints.*;
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
    @Max(value = 999_999_999_999L, message = "주문 가격이 허용 범위를 초과했습니다.")
    private Long orderPrice;
    @NotNull
    @Positive
    @Max(value = 999_999_999L, message = "주문 수량이 허용 범위를 초과했습니다.")
    private Long orderQuantity;
    @NotNull
    private OrderType orderType;
    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
    private String accountPassword;
}

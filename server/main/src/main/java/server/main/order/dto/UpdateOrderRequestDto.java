package server.main.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
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

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
    private String accountPassword;
}

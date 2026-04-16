package server.main.myAccount.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyAccountPasswordRequest {
    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
    private String accountPassword;
}

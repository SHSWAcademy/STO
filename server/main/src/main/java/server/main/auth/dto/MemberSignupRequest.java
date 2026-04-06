package server.main.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class MemberSignupRequest {

    @Email
    @NotBlank
    @Size(max = 100)
    private String email;

    @NotBlank
    @Size(min = 8, max = 100) //최소 8자 강제
    private String password;

    @NotBlank
    @Size(max = 50)
    private String name;

    @NotBlank
    @Pattern(regexp = "\\d{4}", message = "계좌 비밀번호는 4자리 숫자여야 합니다.")
    private String accountPassword;
}

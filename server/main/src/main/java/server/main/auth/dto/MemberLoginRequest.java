package server.main.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class MemberLoginRequest {

    @Email
    @NotBlank
    private String email;

    @NotBlank
    private String password;
}

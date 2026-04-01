package server.main.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class AdminLoginRequest {

    @NotBlank
    private String adminLoginId;

    @NotBlank
    private String password;
}

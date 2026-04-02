package server.main.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class MemberSignupRequest {
    
    @Email
    @NotBlank
    private String email;
    
    @NotBlank
    @Size(min = 8) //최소 8자 강제
    private String password;

    @NotBlank
    private String name;
}

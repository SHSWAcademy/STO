package server.main.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberSignupResponse {
    
    private Long memberId;
    private String email;
    private String name;
    private String walletAddress;
}

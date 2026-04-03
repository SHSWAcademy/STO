package server.main.member.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberMeResponse {
    private Long memberId;
    private String email;
    private String name;
    private String role;
}

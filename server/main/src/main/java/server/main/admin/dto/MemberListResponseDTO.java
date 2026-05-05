package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.LocalDateTime;

@Getter
@Builder
@ToString
public class MemberListResponseDTO {
    private Long memberId;
    private String memberName;
    private String email;
    private LocalDateTime createdAt;   // 가입일
    private Long totalTradeAmount;     // 총 투자액
    private Boolean isActive;          // 상태
}

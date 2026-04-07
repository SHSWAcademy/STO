package server.main.token.dto;

import lombok.*;
import server.main.disclosure.entity.DisclosureCategory;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenDisclosureResponseDto {
    // 상세 페이지 -> 공시
    private String disclosureTitle;     // 공시 제목
    private String disclosureContent;   // 공시본문
    private DisclosureCategory disclosureCategory;  // 공시 타입
    private String OriginName;          // pdf 파일 url (null로 들어갈 수 있다)
    private LocalDateTime createdAt;    // 공시 생성일자 (update 말고 create로 화면에 띄울게요)
}

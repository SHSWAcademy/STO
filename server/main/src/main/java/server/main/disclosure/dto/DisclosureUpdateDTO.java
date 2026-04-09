package server.main.disclosure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.main.disclosure.entity.DisclosureCategory;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DisclosureUpdateDTO {
    private Long disclosureId;      // 공시ID
    private String disclosureTitle;     // 공시 제목
    private String disclosureContent;   // 공시 본문
    private DisclosureCategory disclosureCategory;  // 공시 타입
}

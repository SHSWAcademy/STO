package server.main.disclosure.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.main.disclosure.entity.DisclosureCategory;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DisclosureListResponseDTO {
    private Long disclosureId;          // 공시ID
    private String disclosureTitle;     // 공시 제목
    private String disclosureContent;   // 공시 본문
    private DisclosureCategory disclosureCategory;  // 공시 타입
    private String assetName;           // 자산이름
    private String imgUrl;              // 건물사진
    private String originName;          // 파일 원본명
    private String storedName;          // 파일 저장명
    private LocalDateTime createdAt;    // 공시 작성일
    private LocalDateTime deletedAt;  // 삭제일
}

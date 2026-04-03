package server.main.diclosure.dto;

import lombok.*;
import server.main.diclosure.entity.DisclosureCategory;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class DisclosureRegisterAssetDTO {
    private Long assetId;               // 부동산ID (fk)
    private String disclosureTitle;     // 공시 제목
    private String disclosureContent;   // 공시 본문
    private DisclosureCategory disclosureCategory;  // 공시 타입

    // 자산 등록 시 공시 자동 등록
    public void changeDisclosure(String assetName, Long assetId) {
        this.assetId = assetId;
        this.disclosureCategory = DisclosureCategory.BUILDING;
        this.disclosureTitle = assetName + " 에 관한 자산 안내입니다.";
        this.disclosureContent = "자세한 내용은 첨부파일 참고 바랍니다.";
    }
}

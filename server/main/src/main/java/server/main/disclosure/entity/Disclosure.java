package server.main.disclosure.entity;

import jakarta.persistence.*;
import lombok.*;
import server.main.asset.entity.Asset;
import server.main.global.util.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class Disclosure extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long disclosureId;      // 공시ID
    private String disclosureTitle;     // 공시 제목

    @Column(name = "disclosure_content", columnDefinition = "TEXT")
    private String disclosureContent;   // 공시본문

    @Enumerated(EnumType.STRING)
    private DisclosureCategory disclosureCategory;  // 공시 타입

    private Long assetId;       // 부동산 ID
    private LocalDateTime deletedAt; // 삭제일
    private Boolean isSystem;       // 자동 동록 여부

    // 공시 수정용 매서드
    public void updateDisclosure(String disclosureTitle, String disclosureContent, DisclosureCategory disclosureCategory) {
        this.disclosureTitle = disclosureTitle;
        this.disclosureContent = disclosureContent;
        this.disclosureCategory = disclosureCategory;
    }

    // 삭제용 메서드
    public void softDelete() {
        this.deletedAt = LocalDateTime.now();
    }
}

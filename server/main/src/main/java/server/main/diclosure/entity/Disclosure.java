package server.main.diclosure.entity;

import jakarta.persistence.*;
import lombok.*;
import org.apache.ibatis.annotations.Many;
import server.main.asset.entity.Asset;
import server.main.global.util.BaseEntity;

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

    private Long assetId;       // 부동산ID
}

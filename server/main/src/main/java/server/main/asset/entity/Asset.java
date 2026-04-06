package server.main.asset.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.allocation.entity.AllocationEvent;
import server.main.global.util.BaseEntity;

import java.util.List;


@Entity
@Getter
@Table(name = "ASSETS")
@NoArgsConstructor
@SuperBuilder
public class Asset extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long assetId;
    private Long initPrice;
    private Long totalValue;
    private String assetAddress;
    private String imgUrl;
    private Long totalSupply;
    private String assetName;
    private Boolean isAllocated;

    // 배당 이벤트 테이블 연관관계
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private List<AllocationEvent> allocationEvents;

    // 자산 수정용 메서드 dto -> entity (bgchoi)
    public void updateAsset(String assetName, String assetAddress, String imgUrl) {
        if (assetAddress != null) this.assetAddress = assetAddress;
        if (assetName != null) this.assetName = assetName;
        if (imgUrl != null) this.imgUrl = imgUrl;
    }
}
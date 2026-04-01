package server.main.asset.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.global.util.BaseEntity;

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

    @Column(name = "total_value")
    private Long totalValue;

    @Column(name = "asset_address")
    private String assetAddress;

    @Column(name = "img_url")
    private String imgUrl;

    @Column(name = "asset_name")
    private String assetName;

    @Column(name = "is_allocated")
    private Boolean isAllocated;
}

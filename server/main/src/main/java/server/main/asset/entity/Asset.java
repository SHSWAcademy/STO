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

    private Long initPrice;

    private Long totalValue;

    private String assetAddress;

    private String imgUrl;

    private Long totalSupply;

    private String assetName;

    private Boolean isAllocated;

}

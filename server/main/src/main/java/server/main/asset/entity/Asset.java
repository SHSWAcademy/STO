package server.main.asset.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "ASSETS")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Asset extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "asset_id")
    private Long id;

    private Long initPrice;

    private String assetAddress;

    private String imgUrl;

    private Long totalSupply;

    private String assetName;

    private Boolean isAllocated;

}

package server.main.asset.entity;

import jakarta.persistence.*;
import lombok.*;
import server.main.global.util.BaseEntity;


@Entity
@Getter
@Table(name = "ASSETS")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class Asset extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long assetId;           // 부동산ID
    private Long initPrice;         // 초기토큰 가격
    private String assetAddress;    // 부동산 주소
    private String imgUrl;          // 건물사진 URL
    private Long totalSupply;       // 토큰 발행 총 개수
    private String assetName;       // 자산이름
    private Boolean isAllocated;    // 배당 지급 여부
    private Long totalValue;       // 부동산 총 가격

}

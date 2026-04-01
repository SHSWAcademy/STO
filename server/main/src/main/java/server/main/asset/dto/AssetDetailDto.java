package server.main.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDetailDto {
    // 화면에 전달할 데이터

    private Long assetId;           // 부동산ID
    private Long initPrice;         // 초기토큰 가격
    private String assetAddress;    // 부동산 주소
    private String imgUrl;          // 건물사진 URL
    private Long totalSupply;       // 토큰 발행 총 개수
    private String assetName;       // 자산이름
    private Boolean isAllocated;    // 배당 지급 여부


}

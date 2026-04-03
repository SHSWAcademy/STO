package server.main.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.token.entity.TokenStatus;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetRegisterRequestDTO {
    // Asset 필드
    private String assetAddress;    // 부동산 주소
    private String imgUrl;          // 건물사진 URL
    private Long totalValue;        // 부동산 총 가격
    private String assetName;       // 자산이름
    private Boolean isAllocated;    // 배당 지급 여부

    // token 필드
    private Long initPrice;         // 토큰 초기 가격
    private Long totalSupply;       // 토큰 발행 총 개수
    private Long circulatingSupply; // 토큰 발행 실제 개수
    private String tokenSymbol;     // 토큰 고유이름

    // PlatformTokenHoldings 필드
    private Long holdingSupply;     // 플랫폼 토큰 보유 수량

}
package server.main.admin.dto;

import lombok.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PlatformTokenHoldingsDetailDTO {
    private Long tokenId;               // 토큰ID
    private Long holdingSupply;         // 플랫폼 보유 토큰 수량
    private Long totalSupply;           // 토큰 발행 총 개수
    private Long circulatingSupply;     // 토큰 발행 실제 개수
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 심볼 (줄임 표현)
    private Long initPrice;             // 토큰 초기 가격
    private Double currentPrice;        // 토큰 현재 가격
    private String imgUrl;              // 자산이미지
    private Long allocationAmount;      // 배당수익
}

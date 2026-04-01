package server.main.token.dto;

import lombok.*;
import server.main.token.entity.TokenStatus;

import java.time.LocalDateTime;


// 자산 상세 페이지에 필요한 DTO
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@ToString
public class TokenDetailDto {

    private Long tokenId;               // 토큰ID
    private Long totalSupply;           // 토큰 발행 총 개수
    // private Long circulatingSupply;     // 토큰 발행 실제 개수, 화면에 없음
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 고유이름

    // private String contractAddress;     // 온체인 토큰ID, 화면에 없음
    // private String tokenDecimals;       // ERC20 토큰 메타데이터, 화면에 없음

    private Long initPrice;             // 토큰 초기 가격
    private Long currentPrice;          // 토큰 현재 가격
    private LocalDateTime issuedAt;     // 실제 거래 가능한 상태포 게시된 시간

    private TokenStatus tokenStatus;

    // asset 자산에서 가져올 데이터
    private String assetName;
    private String imgUrl;
    // private Long totalValue; // 화면에 없음
    // private String assetAddress; // 화면에 없음
}

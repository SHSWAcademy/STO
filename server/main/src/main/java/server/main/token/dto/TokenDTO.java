package server.main.token.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import server.main.token.entity.TokenStatus;

import java.time.LocalDateTime;

@Data
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TokenDTO {
    private Long tokenId;               // 토큰ID
    private Long assetId;               // 부동산ID (FK)
    private Long totalSupply;           // 토큰 발행 총 개수
    private Long circulatingSupply;     // 토큰 발행 실제 개수
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 고유이름
    private String contractAddress;     // 온체인 토큰ID
    private String tokenDecimals;       // ERC20 토큰 메타데이터
    private Long initPrice;             // 토큰 초기 가격
    private Long currentPrice;          // 토큰 현재 가격
    private LocalDateTime issuedAt;     // 실제 거래 가능한 상태포 게시된 시간
}

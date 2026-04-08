package server.main.admin.dto;


import lombok.*;
import server.main.token.entity.TokenStatus;

import java.time.LocalDateTime;
import java.time.YearMonth;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AssetListResponseDTO {
    private Long assetId;        // 자산ID
    private String assetName;   // 자산 이름
    private Long totalValue;   // 자산 총 금액
    private TokenStatus status; // 자산 상태 여부
    private String tokenSymbol; // 토큰 심볼 (줄임 표현)
    private String imgUrl;      // 이미지url
    private LocalDateTime issuedAt;    // 발행일자
    private Long totalSupply;       // 토큰 총 발행량
}

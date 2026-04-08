package server.main.admin.dto;

import lombok.*;
import server.main.token.entity.TokenStatus;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AssetDetailResponseDTO {
    // Asset
    private Long assetId;
    private String assetName;
    private String assetAddress;
    private String imgUrl;
    private Long totalValue;
    private Long totalSupply;

    // Token
    private Long tokenId;
    private String tokenName;
    private String tokenSymbol;
    private Long initPrice;
    private Double currentPrice;
    private Long circulatingSupply;
    private TokenStatus tokenStatus;
    private LocalDateTime issuedAt;

    // platform_token_holdings
    private Long holdingSupply;

    // files
    private Long fileId;         // 파일ID
    private String originName;   // 원본 파일명 (화면에 표시)
    private String storedName;   // 저장 파일명 (다운로드 시 사용)

    //disclosure
    private Long disclosureId;
}

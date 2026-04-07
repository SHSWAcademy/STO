package server.main.admin.dto;

import lombok.*;
import server.main.token.entity.TokenStatus;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class AssetUpdateRequestDTO {

    // 자산 수정 대상 : 자산이름, 토큰심볼, 자산 주소, 자산 설명, 자산PDF
    private Long tokenId;               // 토큰ID
    private Long disclosureId;          // 공시ID (파일 조회용)
    private String assetName;           // 자산 이름
    private TokenStatus tokenStatus;    // 토큰 상태
    private String assetAddress;        // 자산 주소
    private String tokenSymbol;         // 토큰 심볼
    private String imgUrl;              // 자산 이미지
    private Boolean isAllocated;        // 배당 지급 여부
}

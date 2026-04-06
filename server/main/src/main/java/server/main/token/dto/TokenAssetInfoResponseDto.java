package server.main.token.dto;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenAssetInfoResponseDto {
    // 상세 페이지 - 종목 정보, originName만 pdfUrl 테이블에서 가져오고 나머지는 Asset 테이블에서 가져온다
    private Long initPrice;          // 초기 가격
    private Long totalValue;         // 총 가치
    private String assetAddress;     // 건물 주소
    private String originName;       // pdf 파일명
    private Long totalSupply;        // 총 토큰 발급량
    private LocalDateTime createdAt; // 상장일
}

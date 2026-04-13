package server.main.token.dto;

import lombok.*;

import java.time.LocalDateTime;


// 자산 상세 페이지에 필요한 DTO
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
public class TokenChartDetailResponseDto {

    private Long tokenId;               // 토큰 ID, for hidden
    private Long totalSupply;           // 토큰 발행 총 개수, 자바 스크립트에서 계산하기 위한 데이터
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 고유이름

    private Long currentPrice;           // 토큰 현재 가격 (초기 로딩용, 이후 웹소켓으로 갱신)
    private LocalDateTime issuedAt;     // 실제 거래 가능한 상태로 게시된 시간

    // asset 자산에서 가져올 데이터
    private String assetName;
    private String imgUrl;
}

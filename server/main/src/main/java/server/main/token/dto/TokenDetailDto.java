package server.main.token.dto;

import lombok.*;
import server.main.token.entity.TokenStatus;

import java.time.LocalDateTime;


// 자산 상세 페이지에 필요한 DTO
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Getter
@Setter
@ToString
public class TokenDetailDto {

    private Long tokenId;               // 토큰ID, for hidden
    private Long totalSupply;           // 토큰 발행 총 개수, 자바 스크립트에서 계산하기 위한 데이터
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 고유이름

    // private Long currentPrice;          // 토큰 현재 가격 -> 웹소켓으로 실시간 변화하기
    private LocalDateTime issuedAt;     // 실제 거래 가능한 상태포 게시된 시간

    // private TokenStatus tokenStatus;    // 화면엔 없지만 후에 필요할 듯 .. ? 해서 일단 넣음 근데 빠질 수도

    // asset 자산에서 가져올 데이터
    private String assetName;
    private String imgUrl;



}

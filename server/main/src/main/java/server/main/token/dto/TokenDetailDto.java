package server.main.token.dto;

import lombok.*;
import server.main.token.entity.TokenStatus;

import java.time.LocalDateTime;


// 자산 상세 페이지에 필요한 DTO
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
@ToString
public class TokenDetailDto {

    private Long tokenId;               // 토큰ID
    private Long totalSupply;           // 토큰 발행 총 개수
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 고유이름

    private Long currentPrice;          // 토큰 현재 가격
    private LocalDateTime issuedAt;     // 실제 거래 가능한 상태포 게시된 시간

    private TokenStatus tokenStatus;

    // asset 자산에서 가져올 데이터
    private String assetName;
    private String imgUrl;


    // candleDay 에서 가져올 1분, 1시간, 1일, 1달, 1년 최고 최저 가격
    private Double highPricePerDay;
    private Double lowPricePerDay;

    private Double highPricePerMonth;
    private Double lowPricePerMonth;

    private Double highPricePerYear;
    private Double lowPricePerYear;

    private Double highPricePerMinute;
    private Double lowPricePerMinute;

    private Double highPricePerHour;
    private Double lowPricePerHour;
}

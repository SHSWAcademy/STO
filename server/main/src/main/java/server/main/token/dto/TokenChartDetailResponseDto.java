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
    // 상세 페이지 조회 시 Http Rest로 조회, 화면에 전달할 데이터

    private Long tokenId;               // 토큰 ID, for hidden
    private Long totalSupply;           // 토큰 발행 총 개수, 자바 스크립트에서 계산하기 위한 데이터
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 고유이름

    private Long currentPrice;           // 토큰 현재 가격 (초기 로딩용, 이후 웹소켓으로 갱신)
    private LocalDateTime issuedAt;     // 실제 거래 가능한 상태로 게시된 시간

    private Long yesterdayClosePrice;   // 전날 종가 (등락률 기준가)

    // asset 자산에서 가져올 데이터
    private String assetName;
    private String imgUrl;

    // candleDay 에서 가져올 데이터
    private Long todayOpenPrice;        // 오늘 시가
    private Long todayHighPrice;        // 오늘 최고가
    private Long todayLowPrice;         // 오늘 최저가

    private String aiSummary;   // 제미나이 요약
    private LocalDateTime aiSummaryUpdatedAt;  // 마지막 업데이트 시간 (선택)
}

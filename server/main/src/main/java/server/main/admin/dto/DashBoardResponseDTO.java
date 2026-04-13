package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.math.BigDecimal;

@Getter
@Builder
@ToString
public class DashBoardResponseDTO {
    // 상단 카드 데이터
    private Long totalUserCount;            // 총 사용자
    private Long dailyExecutionCount;       // 일일 체결수
    private Long totalExecutionCount;       // 누적 체결수
    private Long dailyExecutionAmount;      // 일일 체결 금액
    private Long totalExecutionAmount;      // 누적 체결 금액
    private Long newUserCount;              // 신규 가입자 수
    private Long executionRate;             // 체결률
    private Long openOrderCount;            // 미체결 주문 수

    // 토큰 소유량 분석
    private Long totalTokenIssuedAmount;     // 총 토큰 발행량
    private Long platformOwnedTokenAmount;   // 플랫폼 소유 토큰량
    private Long userOwnedTokenAmount;       // 유저 보유 토큰량

    // 거래내역 리스트

}

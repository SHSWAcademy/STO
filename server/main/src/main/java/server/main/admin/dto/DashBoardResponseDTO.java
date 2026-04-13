package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import org.springframework.data.domain.Page;

import java.math.BigDecimal;
import java.util.List;

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

    // 토큰 소유량 분석
    private List<DashBoardTokenList> tokenList;

    // 거래내역 리스트
    private Page<DashBoardTradeListDTO> tradeList; //거래내역 리스트
}

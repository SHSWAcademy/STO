package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Builder
public class DashBoardTradeListDTO {
    private Long tradeId;            // 거래 완료 ID
    private Long sellerId;           // 매도자 ID
    private Long buyerId;            // 매수자 ID
    private Long tokenId;            // 토큰 ID
    private Long tradePrice;         // 실제 체결 가격 (토큰당)
    private Long tradeQuantity;      // 실제 체결 수량
    private Long totalTradePrice;    // 총 체결 금액
    private Long feeAmount;          // 거래 수수료
    private String settlementStatus;   // 정산 상태 (온체인 대기, 성공, 실패)
    private String tokenName;           // 토큰 이름
    private LocalDateTime executedAt;         // 실제 체결 시간
    private LocalDateTime createdAt;          // 레코드 생성 시간
}

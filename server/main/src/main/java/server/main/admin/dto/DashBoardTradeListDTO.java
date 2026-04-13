package server.main.admin.dto;

import lombok.Builder;
import lombok.Setter;

@Setter
@Builder
public class DashBoardTradeListDTO {
    private String tradeId;            // 거래 완료 ID
    private String sellerId;           // 매도자 ID
    private String buyerId;            // 매수자 ID
    private String sellOrderId;        // 매도 요청 ID
    private String buyOrderId;         // 매수 요청 ID
    private String tokenId;            // 토큰 ID
    private String tradePrice;         // 실제 체결 가격 (토큰당)
    private String tradeQuantity;      // 실제 체결 수량
    private String totalTradePrice;    // 총 체결 금액
    private String feeAmount;          // 거래 수수료
    private String settlementStatus;   // 정산 상태 (온체인 대기, 성공, 실패)
    private String executedAt;         // 실제 체결 시간
    private String createdAt;          // 레코드 생성 시간
}

package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@Getter
@Builder
@ToString
public class TradeStatsResponseDTO {
    private Long totalTx; // 총 체결수
    private Long pendingCount;  // 오프체인 카운트
    private Long successCount;  // 온체인 카운트
    private Long totalAmount;   // 총 체결 금액

    List<TokenStatsDTO> tokenStatsList;
}

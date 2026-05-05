package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TokenStatsDTO {
    private Long tokenId;       // 토큰ID
    private String tokenSymbol; // 토큰 심볼
    private Long count;         // 카운트
    private Long pending;       // 오프체인
    private Long amount;        // 총 금액
    private String contract_address; // 블록체인 주소
}

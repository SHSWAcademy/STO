package server.main.token.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenMainResponseDto {  // 메인 페이지에 띄워줄 자산 리스트
    private Long token;              // 상세 페이지로 접근할 수 있도록 화면에 전달, hidden
    private String assetName;        // 자산 이름
    private Long currentPrice;       // 현재 가격
    private Double fluctuationRate;  // 등락률
    private Long totalTradeValue;   // 총 거래 대금
    private Long totalTradeQuantity; // 총 거래 수량
}

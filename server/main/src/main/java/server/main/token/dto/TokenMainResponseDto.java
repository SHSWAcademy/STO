package server.main.token.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenMainResponseDto {
    private Long tokenId;
    private String assetName;
    private Long basePrice;
    private Long currentPrice;
    private Double fluctuationRate;
    private Long totalTradeValue;
    private Long totalTradeQuantity;
    private List<SparkPointDto> sparkLine;
    private String tokenSymbol;
    private String imgUrl;
    private String aiSummary;
    private LocalDateTime aiSummaryUpdatedAt;
    private Long totalSupply;
    private Long circulatingSupply;
    private Long initPrice;
}
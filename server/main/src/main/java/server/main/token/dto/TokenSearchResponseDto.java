package server.main.token.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenSearchResponseDto {
    private Long tokenId;
    private String tokenName;
    private String tokenSymbol;
    private Long currentPrice;
    private Double fluctuationRate;
    private String imgUrl;
}

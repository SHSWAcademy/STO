package server.main.token.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopMoverDto {
    private Long tokenId;
    private String assetName;
    private String tokenSymbol;
    private Double fluctuationRate;
    private Long currentPrice;
    private String imgUrl;
}

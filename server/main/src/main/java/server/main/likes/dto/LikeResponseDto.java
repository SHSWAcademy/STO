package server.main.likes.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikeResponseDto {
    private Long tokenId;
    private Long assetId;
    private String assetName;
    private String tokenSymbol;
    private Long currentPrice;
    private String imgUrl;
}

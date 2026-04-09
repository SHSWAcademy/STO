package server.main.asset.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMainResponseDto {
    private Long assetId;
    private String assetName;
    private String imgUrl;
    private Long initPrice;
    private Long totalValue;
    private Long totalSupply;
    private Boolean isAllocated;

}

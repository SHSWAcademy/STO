package server.main.asset.dto;

import lombok.*;
import server.main.asset.entity.Asset;

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

    // Asset -> DTO 변환 정적 팩토리 메서드
    public static AssetMainResponseDto toMainDto(Asset asset) {
        return AssetMainResponseDto.builder()
                .assetId(asset.getAssetId())
                .assetName(asset.getAssetName())
                .imgUrl(asset.getImgUrl())
                .initPrice(asset.getInitPrice())
                .totalValue(asset.getTotalValue())
                .totalSupply(asset.getTotalSupply())
                .isAllocated(asset.getIsAllocated())
                .build();
    }
}

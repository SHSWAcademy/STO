package server.main.asset.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssetDetailDto {

    // 화면에 전달할 데이터
    private Long assetId;

    private Long initPrice;

    private String assetAddress;

    private String imgUrl;

    private Long totalSupply;

    private String assetName;

    private Boolean isAllocated;

}

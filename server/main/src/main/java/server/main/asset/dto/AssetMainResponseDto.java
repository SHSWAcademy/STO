package server.main.asset.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssetMainResponseDto { // 메인 페이지에 띄워줄 자산 리스트
    private Long assetId;       // id
    private String assetName;   // 자산 이름

}

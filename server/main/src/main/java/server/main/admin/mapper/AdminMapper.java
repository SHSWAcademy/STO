package server.main.admin.mapper;

import org.springframework.stereotype.Component;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.asset.entity.Asset;
import server.main.token.entity.Token;
import server.main.token.entity.TokenStatus;

@Component
public class AdminMapper {

    // 토큰 첫 발생 시 dto -> entity 변환
//    public Token toToken(AssetRegisterRequestDTO dto) {
//        return Token.builder()
//                .totalSupply(dto.getTotalSupply())
//                .tokenName(dto.getAssetName())
//                .currentPrice(dto.getInitPrice())
//                .tokenSymbol(dto.getTokenSymbol())
//                .initPrice(dto.getInitPrice())
//                .tokenStatus(TokenStatus.ISSUED)
//                .build();
//    }

//    // 자산 첫 등록 시 dto -> entity 변환
//    public Asset toAsset(AssetRegisterRequestDTO dto) {
//        return Asset.builder()
//                .initPrice(dto.getInitPrice())
//                .assetAddress(dto.getAssetAddress())
//                .imgUrl(dto.getImgUrl())
//                .totalSupply(dto.getTotalSupply())
//                .assetName(dto.getAssetName())
//                .isAllocated(dto.getIsAllocated())
//                .totalValue(dto.getTotalValue())
//                .build();
//    }
//
//    // 자산 첫 등록시 플랫폼 보유수랑 설정을위한 entity
//    public PlatformTokenHolding toPlatformTokenHoldings(AssetRegisterRequestDTO dto) {
//        return PlatformTokenHolding.builder()
//                .holdingSupply(dto.getHoldingSupply())
//                .initPrice(dto.getInitPrice())
//                .build();
//    }
}

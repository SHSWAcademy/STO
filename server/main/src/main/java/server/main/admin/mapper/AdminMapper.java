package server.main.admin.mapper;

import org.springframework.stereotype.Component;
import server.main.admin.dto.AssetDetailResponseDTO;
import server.main.admin.dto.AssetListResponseDTO;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.asset.entity.Asset;
import server.main.token.entity.Token;
import server.main.token.entity.TokenStatus;

@Component
public class AdminMapper {

    // 토큰 첫 발생 시 dto -> entity 변환
    public Token toToken(AssetRegisterRequestDTO dto, Asset asset) {
        return Token.builder()
                .totalSupply(dto.getTotalSupply())
                .asset(asset)
                .tokenName(dto.getAssetName())
                .currentPrice(dto.getInitPrice())
                .circulatingSupply(dto.getCirculatingSupply())
                .tokenSymbol(dto.getTokenSymbol())
                .initPrice(dto.getInitPrice())
                .tokenStatus(TokenStatus.ISSUED)
                .build();
    }

    // 자산 첫 등록 시 dto -> entity 변환
    public Asset toAsset(AssetRegisterRequestDTO dto, String imgUrl) {
        return Asset.builder()
                .initPrice(dto.getInitPrice())
                .assetAddress(dto.getAssetAddress())
                .imgUrl(imgUrl)
                .totalSupply(dto.getTotalSupply())
                .assetName(dto.getAssetName())
                .isAllocated(dto.getIsAllocated())
                .totalValue(dto.getTotalValue())
                .build();
    }

    // 자산 첫 등록시 플랫폼 보유수랑 설정을위한 dto -> entity 변환
    public PlatformTokenHolding toPlatformTokenHoldings(AssetRegisterRequestDTO dto, Token token) {
        return PlatformTokenHolding.builder()
                .holdingSupply(dto.getHoldingSupply())
                .token(token)
                .initPrice(dto.getInitPrice())
                .build();
    }

    // 자산 상세조회 entity -> dto 변환
    public AssetDetailResponseDTO toAssetDetailResponseDTO(PlatformTokenHolding holding) {
        Token token = holding.getToken();
        Asset asset = token.getAsset();
        return AssetDetailResponseDTO.builder()
                .assetId(asset.getAssetId())
                .assetName(asset.getAssetName())
                .assetAddress(asset.getAssetAddress())
                .imgUrl(asset.getImgUrl())
                .totalValue(asset.getTotalValue())
                .totalSupply(asset.getTotalSupply())
                .tokenId(token.getTokenId())
                .tokenName(token.getTokenName())
                .tokenSymbol(token.getTokenSymbol())
                .initPrice(token.getInitPrice())
                .currentPrice(token.getCurrentPrice())
                .circulatingSupply(token.getCirculatingSupply())
                .tokenStatus(token.getTokenStatus())
                .issuedAt(token.getIssuedAt())
                .holdingSupply(holding.getHoldingSupply())
                .build();
    }

    // 자산 리스트 조회 entity -> dto 변환
    public AssetListResponseDTO toAssetListResponseDTO(Token token) {
        return AssetListResponseDTO.builder()
                .assetId(token.getAsset().getAssetId())
                .assetName(token.getAsset().getAssetName())
                .totalValue(token.getAsset().getTotalValue())
                .status(token.getTokenStatus())
                .tokenSymbol(token.getTokenSymbol())
                .build();
    }
}

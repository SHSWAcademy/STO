package server.main.admin.mapper;

import org.springframework.stereotype.Component;
import server.main.admin.dto.*;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.allocation.entity.AllocationEvent;
import server.main.asset.entity.Asset;
import server.main.global.file.File;
import server.main.token.entity.Token;
import server.main.token.entity.TokenStatus;

import java.time.LocalDate;
import java.time.YearMonth;

@Component
public class AdminMapper {

    // 토큰 첫 발생 시 dto -> entity 변환
    public Token toToken(AssetRegisterRequestDTO dto, Asset asset) {
        return Token.builder()
                .totalSupply(dto.getTotalSupply())
                .asset(asset)
                .tokenName(dto.getAssetName())
                .currentPrice(Double.valueOf(dto.getInitPrice()))
                .circulatingSupply(dto.getTotalSupply() - dto.getHoldingSupply())   // 전체개수 - 플랫폼 소유 갯수
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
    public AssetDetailResponseDTO toAssetDetailResponseDTO(PlatformTokenHolding holding, File file, Long disclosureId) {
        Token token = holding.getToken();
        Asset asset = token.getAsset();
        return AssetDetailResponseDTO.builder()
                .assetId(asset.getAssetId())
                .disclosureId(disclosureId)
                .assetName(asset.getAssetName())
                .assetAddress(asset.getAssetAddress())
                .imgUrl(asset.getImgUrl())
                .isAllocated(asset.getIsAllocated())
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
                .fileId(file.getFileId())
                .originName(file.getOrigin_name())
                .storedName(file.getStored_name())
                .build();
    }

    // 자산 리스트 조회 entity -> dto 변환
    public AssetListResponseDTO toAssetListResponseDTO(Token token) {
        return AssetListResponseDTO.builder()
                .assetId(token.getAsset().getAssetId())
                .assetName(token.getAsset().getAssetName())
                .totalValue(token.getAsset().getTotalValue())
                .status(token.getTokenStatus())
                .isAllocated(token.getAsset().getIsAllocated())
                .tokenSymbol(token.getTokenSymbol())
                .imgUrl(token.getAsset().getImgUrl())
                .issuedAt(token.getIssuedAt())
                .totalSupply(token.getTotalSupply())
                .build();
    }

    // 베당 리스트 조회 (기존 자산리스트 + allocation 테이블 합쳐서)
    public AllocationListResponseDTO toAllocationListResponseDTO(Token token, AllocationEvent allocationEvent, YearMonth targetMonth, LocalDate adminTargetMonth) {
        return AllocationListResponseDTO.builder()
                .assetId(token.getAsset().getAssetId())
                .assetName(token.getAsset().getAssetName())
                .imgUrl(token.getAsset().getImgUrl())
                .tokenSymbol(token.getTokenSymbol())
                // null 검증 (배당등록이 안되어있으면 null임)
                .monthlyDividendIncome(allocationEvent != null ? allocationEvent.getMonthlyDividendIncome() : null)
                .allocationBatchStatus(allocationEvent != null ? allocationEvent.getAllocationBatchStatus() : null)
                .targetMonth(targetMonth)
                .allocateSetMonth(adminTargetMonth)
                .build();
    }

    // 배당 상세내역 리스트 entity -> dto
    public AllocationDetailResponseDTO toAllocationDetailResponseDTO(AllocationEvent dto, File file) {
        return AllocationDetailResponseDTO.builder()
                .allocationEventId(dto.getAllocationEventId())
                .disclosureId(dto.getDisclosureId())
                .allocationBatchStatus(dto.getAllocationBatchStatus())
                .monthlyDividendIncome(dto.getMonthlyDividendIncome())
                .settled_at(dto.getSettledAt())
                .settlementMonth(dto.getSettlementMonth())
                .settlementYear(dto.getSettlementYear())
                .storedName(file.getOrigin_name())
                .originName(file.getOrigin_name())
                .build();
    }


}

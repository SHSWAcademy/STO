package server.main.admin.mapper;

import org.springframework.stereotype.Component;
import server.main.admin.dto.*;
import server.main.admin.entity.PlatformBanking;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.allocation.entity.AllocationEvent;
import server.main.asset.entity.Asset;
import server.main.global.file.File;
import server.main.member.entity.Member;
import server.main.token.entity.Token;
import server.main.token.entity.TokenStatus;
import server.main.trade.entity.Trade;

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
                .currentPrice(dto.getInitPrice())
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
                .fileId(file != null ? file.getFileId() : null)
                .originName(file != null ? file.getOriginName() : null)
                .storedName(file != null ? file.getStoredName() : null)
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
                .settledAt(dto.getSettledAt())
                .settlementMonth(dto.getSettlementMonth())
                .settlementYear(dto.getSettlementYear())
                .storedName(file != null ? file.getStoredName() : null)
                .originName(file != null ? file.getOriginName() : null)
                .build();
    }

    // 플랫폼 계좌내역 entity -> dto변환
    public PlatformBankingListDTO toPlatformBankingListDTO(PlatformBanking platformBanking) {
        return PlatformBankingListDTO.builder()
                .platformBankingDirection(platformBanking.getPlatformBankingDirection())
                .accountType(platformBanking.getAccountType())
                .platformBankingAmount(platformBanking.getPlatformBankingAmount())
                .createdAt(platformBanking.getCreatedAt())
                .tokenId(platformBanking.getTokenId())
                .build();
    }

    // 플랫폼 보유 토큰 상세 entity -> dto 변환
    public PlatformTokenHoldingsDetailDTO toPlatformTokenHoldingsDetailDTO(PlatformTokenHolding platformTokenHolding) {
        return PlatformTokenHoldingsDetailDTO.builder()
                .tokenName(platformTokenHolding.getToken().getTokenName())
                .tokenSymbol(platformTokenHolding.getToken().getTokenSymbol())
                .totalSupply(platformTokenHolding.getToken().getTotalSupply())
                .initPrice(platformTokenHolding.getInitPrice())
                .circulatingSupply(platformTokenHolding.getToken().getCirculatingSupply())
                .currentPrice(platformTokenHolding.getToken().getCurrentPrice())
                .holdingSupply(platformTokenHolding.getHoldingSupply())
                .imgUrl(platformTokenHolding.getToken().getAsset().getImgUrl())
                .tokenId(platformTokenHolding.getToken().getTokenId())
                .build();
    }

    // 멤버 리스트 관리 entity -> dto 변환
    public MemberListResponseDTO toMemberListResponseDTO(Member member, Long totalAmount) {
        return MemberListResponseDTO.builder()
                .memberId(member.getMemberId())
                .email(member.getEmail())
                .memberName(member.getMemberName())
                .isActive(member.getIsActive())
                .createdAt(member.getCreatedAt())
                .totalTradeAmount(totalAmount)
                .build();
    }

    // 거래내역 entity -> dto 변환
    public DashBoardTradeListDTO toDashBoardTradeListDTO(Trade trade) {
        return DashBoardTradeListDTO.builder()
                .tradeId(trade.getTradeId())
                .feeAmount(trade.getFeeAmount())
                .tradePrice(trade.getTradePrice())
                .tradeQuantity(trade.getTradeQuantity())
                .sellerId(trade.getSeller().getMemberId())
                .buyerId(trade.getBuyer().getMemberId())
                .sellerName(trade.getBuyer().getMemberName())
                .buyerName(trade.getBuyer().getMemberName())
                .settlementStatus(String.valueOf(trade.getSettlementStatus()))
                .totalTradePrice(trade.getTotalTradePrice())
                .executedAt(trade.getExecutedAt())
                .createdAt(trade.getCreatedAt())
                .tokenId(trade.getToken().getTokenId())
                .tokenName(trade.getToken().getTokenName())
                .build();
    }

    // 대시보드 토큰 리스트 entity -> dto변환
    public DashBoardTokenList toDashBoardTokenList(Token token, Long currentQuantity){
        return DashBoardTokenList.builder()
                .tokenId(token.getTokenId())
                .tokenSymbol(token.getTokenSymbol())
                .tokenName(token.getTokenName())
                .totalSupply(token.getTotalSupply())
                .holdingSupply(token.getTotalSupply() - token.getCirculatingSupply())
                .currentQuantity(currentQuantity)
                .build();
    }
}

package server.main.token.service;

import server.main.token.dto.SelectType;
import server.main.token.dto.*;

import java.util.List;

public interface TokenService {
    TokenChartDetailResponseDto getTokenDetail(Long assetId);

    TokenAssetInfoResponseDto getTokenAssetInfo(Long tokenId);

    List<TokenAllocationInfoResponseDto> getAllocationInfo(Long tokenId);

    List<TokenDisclosureResponseDto> getDisclosureInfo(Long tokenId);

    List<TokenMainResponseDto> getTokenAssetsWith10Paging(int page, SelectType selectType, PeriodType periodType);

    long getTickSize(Long tokenId);

}

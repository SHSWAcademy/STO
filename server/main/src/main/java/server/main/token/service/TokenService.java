package server.main.token.service;

import server.main.token.dto.SelectType;
import server.main.token.dto.*;

import java.util.List;
import java.util.Map;

public interface TokenService {
    TokenChartDetailResponseDto getTokenDetail(Long assetId);

    TokenAssetInfoResponseDto getTokenAssetInfo(Long tokenId);

    List<TokenAllocationInfoResponseDto> getAllocationInfo(Long tokenId);

    List<TokenDisclosureResponseDto> getDisclosureInfo(Long tokenId);

    List<TokenMainResponseDto> getTokenAssetsWith10Paging(int page, SelectType selectType, PeriodType periodType);

    Map<Long, List<SparkPointDto>> getSparklines(List<Long> tokenIds, PeriodType periodType);

    long getTickSize(Long tokenId);

    String getAssetName(Long tokenId);

    List<TokenSearchResponseDto> searchTokens(String keyword);

}

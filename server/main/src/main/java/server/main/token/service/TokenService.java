package server.main.token.service;

import server.main.token.dto.TokenAllocationInfoResponseDto;
import server.main.token.dto.TokenAssetInfoResponseDto;
import server.main.token.dto.TokenChartDetailResponseDto;
import server.main.token.dto.TokenDisclosureResponseDto;

import java.util.List;

public interface TokenService {
    TokenChartDetailResponseDto getTokenDetail(Long assetId);

    TokenAssetInfoResponseDto getTokenAssetInfo(Long tokenId);

    List<TokenAllocationInfoResponseDto> getAllocationInfo(Long tokenId);

    List<TokenDisclosureResponseDto> getDisclosureInfo(Long tokenId);
}

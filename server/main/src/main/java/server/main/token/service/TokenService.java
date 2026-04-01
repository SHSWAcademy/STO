package server.main.token.service;

import server.main.token.dto.TokenDetailDto;

public interface TokenService {
    TokenDetailDto getTokenDetail(Long assetId);
}

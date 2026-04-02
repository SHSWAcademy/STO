package server.main.token.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.candle.dto.PriceStatsDto;
import server.main.candle.service.CandleService;
import server.main.token.dto.TokenDetailDto;
import server.main.token.entity.Token;
import server.main.token.mapper.TokenMapper;
import server.main.token.repository.TokenRepository;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService{

    private final TokenRepository tokenRepository;
    private final TokenMapper tokenMapper;


    @Override
    public TokenDetailDto getTokenDetail(Long tokenId) {

        // 토큰, 자산 데이터 세팅
        Token findToken = tokenRepository.findByIdWithAsset(tokenId)
                .orElseThrow(() -> new EntityNotFoundException("cannot find entity"));

        TokenDetailDto dto = tokenMapper.toDtoDetail(findToken);

        return dto;
    }
}

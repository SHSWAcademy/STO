package server.main.token.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.security.CustomUserPrincipal;
import server.main.member.entity.Member;
import server.main.member.repository.MemberRepository;
import server.main.order.entity.Order;
import server.main.token.dto.TokenDetailDto;
import server.main.token.entity.Token;
import server.main.token.mapper.TokenMapper;
import server.main.token.repository.TokenRepository;

import java.util.List;

import static server.main.global.error.ErrorCode.ENTITY_NOT_FOUNT_ERROR;

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
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        TokenDetailDto dto = tokenMapper.toDtoDetail(findToken);

        return dto;
    }
}

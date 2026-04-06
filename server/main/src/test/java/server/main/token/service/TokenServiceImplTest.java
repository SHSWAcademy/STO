package server.main.token.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.main.global.error.BusinessException;
import server.main.token.dto.TokenDetailDto;
import server.main.token.entity.Token;
import server.main.token.mapper.TokenMapper;
import server.main.token.repository.TokenRepository;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock
    TokenRepository tokenRepository;

    @Mock
    TokenMapper tokenMapper;

    @InjectMocks
    TokenServiceImpl tokenService;

    @Test
    void getTokenDetail_정상조회() {
        // given
        Token token = new Token();
        TokenDetailDto dto = TokenDetailDto.builder()
                .tokenId(1L)
                .tokenName("서울 빌딩")
                .tokenSymbol("SEOUL")
                .build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(tokenMapper.toDtoDetail(token)).thenReturn(dto);

        // when
        TokenDetailDto result = tokenService.getTokenDetail(1L);

        // then
        assertThat(result.getTokenId()).isEqualTo(1L);
        assertThat(result.getTokenName()).isEqualTo("서울 빌딩");
        assertThat(result.getTokenSymbol()).isEqualTo("SEOUL");
        verify(tokenRepository).findByIdWithAsset(1L);
        verify(tokenMapper).toDtoDetail(token);
    }

    @Test
    void getTokenDetail_예외() {
        // given : 999번 조회 시 empty 리턴
        when(tokenRepository.findByIdWithAsset(999L)).thenReturn(Optional.empty());

        // when & then
        assertThrows(BusinessException.class,
                () -> tokenService.getTokenDetail(999L));
        verify(tokenRepository).findByIdWithAsset(999L);
    }
}

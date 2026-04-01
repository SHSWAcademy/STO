package server.main.token.service;

import jakarta.persistence.EntityNotFoundException;
import org.assertj.core.api.AbstractBigIntegerAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

    @Test // 조회 테스트
    void getTokenDetail_조회성공() {
        //given
        Token token = new Token();

        TokenDetailDto dto = TokenDetailDto.builder() // dto 생성
                .tokenId(1L)
                .tokenName("서울 빌딩")
                .tokenSymbol("hello")
                .currentPrice(1230L)
                .build();

        // 상황 가정
        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(tokenMapper.toDtoDetail(token)).thenReturn(dto);

        //when

        TokenDetailDto result = tokenService.getTokenDetail(1L);

        //then
        assertThat(result.getTokenId()).isEqualTo(1L);
        assertThat(result.getTokenSymbol()).isEqualTo("hello");
        verify(tokenRepository).findByIdWithAsset(1L);
        verify(tokenMapper).toDtoDetail(token);
    }

    @Test
    void getTokenDetail_조회실패() {
        // given
        Long tokenId = 999L;

        // 상황 가정
        when(tokenRepository.findByIdWithAsset(tokenId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(EntityNotFoundException.class,
                () -> tokenService.getTokenDetail(tokenId));
    }
}
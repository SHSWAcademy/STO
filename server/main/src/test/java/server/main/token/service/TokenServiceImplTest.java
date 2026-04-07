package server.main.token.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.main.allocation.entity.AllocationEvent;
import server.main.allocation.repository.AllocationEventRepository;
import server.main.asset.entity.Asset;
import server.main.disclosure.entity.Disclosure;
import server.main.disclosure.entity.DisclosureCategory;
import server.main.disclosure.repository.DisclosureRepository;
import server.main.global.error.BusinessException;
import server.main.global.file.File;
import server.main.global.file.FileRepository;
import server.main.token.dto.TokenAllocationInfoResponseDto;
import server.main.token.dto.TokenAssetInfoResponseDto;
import server.main.token.dto.TokenChartDetailResponseDto;
import server.main.token.dto.TokenDisclosureResponseDto;
import server.main.token.entity.Token;
import server.main.token.mapper.TokenMapper;
import server.main.token.repository.TokenRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock TokenRepository tokenRepository;
    @Mock TokenMapper tokenMapper;
    @Mock DisclosureRepository disclosureRepository;
    @Mock FileRepository fileRepository;
    @Mock AllocationEventRepository allocationEventRepository;

    @InjectMocks
    TokenServiceImpl tokenService;


    @Test
    void getTokenDetail_정상조회() {
        Token token = Token.builder().tokenId(1L).tokenName("서울 빌딩").tokenSymbol("SEOUL").build();
        TokenChartDetailResponseDto dto = TokenChartDetailResponseDto.builder()
                .tokenId(1L).tokenName("서울 빌딩").tokenSymbol("SEOUL").build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(tokenMapper.toDtoDetail(token)).thenReturn(dto);

        TokenChartDetailResponseDto result = tokenService.getTokenDetail(1L);

        assertThat(result.getTokenId()).isEqualTo(1L);
        assertThat(result.getTokenName()).isEqualTo("서울 빌딩");
        assertThat(result.getTokenSymbol()).isEqualTo("SEOUL");
        verify(tokenRepository).findByIdWithAsset(1L);
        verify(tokenMapper).toDtoDetail(token);
    }

    @Test
    void getTokenDetail_토큰없음_예외() {
        when(tokenRepository.findByIdWithAsset(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tokenService.getTokenDetail(999L));
        verify(tokenRepository).findByIdWithAsset(999L);
    }


    @Test
    void getTokenAssetInfo_정상조회() {
        Asset asset = Asset.builder()
                .assetId(1L)
                .initPrice(10000L)
                .totalValue(500000000L)
                .assetAddress("서울시 강남구 테헤란로 123")
                .totalSupply(50000L)
                .build();
        Token token = Token.builder().tokenId(1L).asset(asset).build();

        Disclosure disclosure = Disclosure.builder()
                .disclosureId(10L)
                .disclosureCategory(DisclosureCategory.BUILDING)
                .assetId(1L)
                .build();
        File file = File.builder()
                .fileId(100L)
                .disclosureId(10L)
                .origin_name("건물_소개서.pdf")
                .build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findByAssetIdAndCategory(1L)).thenReturn(Optional.of(disclosure));
        when(fileRepository.findByDisclosureId(10L)).thenReturn(file);

        TokenAssetInfoResponseDto result = tokenService.getTokenAssetInfo(1L);

        assertThat(result.getInitPrice()).isEqualTo(10000L);
        assertThat(result.getTotalValue()).isEqualTo(500000000L);
        assertThat(result.getAssetAddress()).isEqualTo("서울시 강남구 테헤란로 123");
        assertThat(result.getOriginName()).isEqualTo("건물_소개서.pdf");
        assertThat(result.getTotalSupply()).isEqualTo(50000L);
    }

    @Test
    void getTokenAssetInfo_토큰없음_예외() {
        when(tokenRepository.findByIdWithAsset(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tokenService.getTokenAssetInfo(999L));
    }

    @Test
    void getTokenAssetInfo_BUILDING_공시없음_예외() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).asset(asset).build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findByAssetIdAndCategory(1L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tokenService.getTokenAssetInfo(1L));
    }



    @Test
    void getAllocationInfo_정상조회() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).totalSupply(1000L).asset(asset).build();

        LocalDateTime date1 = LocalDateTime.of(2024, 3, 20, 0, 0);
        LocalDateTime date2 = LocalDateTime.of(2023, 12, 20, 0, 0);
        List<AllocationEvent> events = List.of(
                AllocationEvent.builder().allocationEventId(1L).monthlyDividendIncome(500000L).settledAt(date1).allocationBatchStatus(true).build(),
                AllocationEvent.builder().allocationEventId(2L).monthlyDividendIncome(480000L).settledAt(date2).allocationBatchStatus(true).build()
        );

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(allocationEventRepository.findAllByAssetIdOrderBySettledAtDesc(1L)).thenReturn(events);

        List<TokenAllocationInfoResponseDto> result = tokenService.getAllocationInfo(1L);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getSettledAt()).isEqualTo(date1);
        assertThat(result.get(0).getMonthlyDividendIncome()).isEqualTo(500000L);
        assertThat(result.get(0).getAllocationPerToken()).isEqualTo(500L);   // 500000 / 1000
        assertThat(result.get(0).getAllocationBatchStatus()).isTrue();
    }

    @Test
    void getAllocationInfo_배당이벤트없음_빈리스트반환() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).totalSupply(1000L).asset(asset).build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(allocationEventRepository.findAllByAssetIdOrderBySettledAtDesc(1L)).thenReturn(List.of());

        List<TokenAllocationInfoResponseDto> result = tokenService.getAllocationInfo(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getAllocationInfo_totalSupply가0이면_perToken은0() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).totalSupply(0L).asset(asset).build(); // total supply = 0

        List<AllocationEvent> events = List.of(
                AllocationEvent.builder().allocationEventId(1L).monthlyDividendIncome(500000L)
                        .settledAt(LocalDateTime.now()).allocationBatchStatus(true).build()
        );

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(allocationEventRepository.findAllByAssetIdOrderBySettledAtDesc(1L)).thenReturn(events);

        List<TokenAllocationInfoResponseDto> result = tokenService.getAllocationInfo(1L);

        assertThat(result.get(0).getAllocationPerToken()).isEqualTo(0L);
    }

    @Test
    void getAllocationInfo_totalSupplyNull이면_perToken은0() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).totalSupply(null).asset(asset).build(); // total supply = null

        List<AllocationEvent> events = List.of(
                AllocationEvent.builder().allocationEventId(1L).monthlyDividendIncome(500000L)
                        .settledAt(LocalDateTime.now()).allocationBatchStatus(false).build()
        );

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(allocationEventRepository.findAllByAssetIdOrderBySettledAtDesc(1L)).thenReturn(events);

        List<TokenAllocationInfoResponseDto> result = tokenService.getAllocationInfo(1L);

        assertThat(result.get(0).getAllocationPerToken()).isEqualTo(0L);
    }

    @Test
    void getAllocationInfo_토큰없음_예외() {
        when(tokenRepository.findByIdWithAsset(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tokenService.getAllocationInfo(999L));
    }


    @Test
    void getDisclosureInfo_정상조회_파일있음() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).asset(asset).build();

        Disclosure disclosure = Disclosure.builder()
                .disclosureId(10L)
                .disclosureTitle("2024년 1분기 배당 공시")
                .disclosureContent("배당금 지급 안내")
                .disclosureCategory(DisclosureCategory.DIVIDEND)
                .assetId(1L)
                .build();
        File file = File.builder()
                .fileId(100L)
                .disclosureId(10L)
                .origin_name("공시문서.pdf")
                .build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findAllByAssetId(1L)).thenReturn(List.of(disclosure));
        when(fileRepository.findByDisclosureId(10L)).thenReturn(file);

        List<TokenDisclosureResponseDto> result = tokenService.getDisclosureInfo(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDisclosureTitle()).isEqualTo("2024년 1분기 배당 공시");
        assertThat(result.get(0).getDisclosureCategory()).isEqualTo(DisclosureCategory.DIVIDEND);
        assertThat(result.get(0).getOriginName()).isEqualTo("공시문서.pdf");
    }

    @Test
    void getDisclosureInfo_파일X공시_OriginNameNull() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).asset(asset).build();

        Disclosure disclosure = Disclosure.builder()
                .disclosureId(10L)
                .disclosureTitle("건물 소개")
                .disclosureCategory(DisclosureCategory.BUILDING)
                .assetId(1L)
                .build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findAllByAssetId(1L)).thenReturn(List.of(disclosure));
        when(fileRepository.findByDisclosureId(10L)).thenReturn(null);  // 파일 없음

        List<TokenDisclosureResponseDto> result = tokenService.getDisclosureInfo(1L);

        assertThat(result.get(0).getOriginName()).isNull(); // assertThat ~ .isNull은 null 인지 확인, assertThrows는 예외가 터졌는지 확인
    }

    @Test
    void getDisclosureInfo_공시없음_빈리스트반환() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).asset(asset).build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findAllByAssetId(1L)).thenReturn(List.of());

        List<TokenDisclosureResponseDto> result = tokenService.getDisclosureInfo(1L);

        assertThat(result).isEmpty();
    }

    @Test
    void getDisclosureInfo_토큰없음_예외() {
        when(tokenRepository.findByIdWithAsset(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tokenService.getDisclosureInfo(999L));
    }
}

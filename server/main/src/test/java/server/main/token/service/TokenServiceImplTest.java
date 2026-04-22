package server.main.token.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.main.allocation.entity.AllocationEvent;
import server.main.allocation.repository.AllocationEventRepository;
import server.main.asset.entity.Asset;
import server.main.candle.entity.CandleDay;
import server.main.candle.repository.CandleDayRepository;
import server.main.candle.repository.CandleMonthRepository;
import server.main.candle.repository.CandleYearRepository;
import server.main.candle.service.CandleLiveManager;
import server.main.disclosure.entity.Disclosure;
import server.main.disclosure.entity.DisclosureCategory;
import server.main.disclosure.repository.DisclosureRepository;
import server.main.global.error.BusinessException;
import server.main.global.file.File;
import server.main.global.file.FileRepository;
import server.main.global.util.GeminiClient;
import server.main.token.dto.PeriodType;
import server.main.token.dto.SelectType;
import server.main.token.dto.TokenAllocationInfoResponseDto;
import server.main.token.dto.TokenAssetInfoResponseDto;
import server.main.token.dto.TokenChartDetailResponseDto;
import server.main.token.dto.TokenDisclosureResponseDto;
import server.main.token.dto.TokenMainResponseDto;
import server.main.token.entity.Token;
import server.main.token.mapper.TokenMapper;
import server.main.token.repository.TokenRepository;
import server.main.trade.repository.TradeRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenServiceImplTest {

    @Mock TokenRepository tokenRepository;
    @Mock TokenMapper tokenMapper;
    @Mock DisclosureRepository disclosureRepository;
    @Mock FileRepository fileRepository;
    @Mock AllocationEventRepository allocationEventRepository;
    @Mock TradeRepository tradeRepository;
    @Mock CandleDayRepository candleDayRepository;
    @Mock CandleMonthRepository candleMonthRepository;
    @Mock CandleYearRepository candleYearRepository;
    @Mock CandleLiveManager candleLiveManager;
    @Mock GeminiClient geminiClient;

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
                .originName("건물_공시.pdf")
                .build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findByAssetIdAndCategory(1L)).thenReturn(Optional.of(disclosure));
        when(fileRepository.findByDisclosureId(10L)).thenReturn(file);

        TokenAssetInfoResponseDto result = tokenService.getTokenAssetInfo(1L);

        assertThat(result.getInitPrice()).isEqualTo(10000L);
        assertThat(result.getTotalValue()).isEqualTo(500000000L);
        assertThat(result.getAssetAddress()).isEqualTo("서울시 강남구 테헤란로 123");
        assertThat(result.getOriginName()).isEqualTo("건물_공시.pdf");
        assertThat(result.getTotalSupply()).isEqualTo(50000L);
    }

    @Test
    void getTokenAssetInfo_토큰없음_예외() {
        when(tokenRepository.findByIdWithAsset(999L)).thenReturn(Optional.empty());

        assertThrows(BusinessException.class, () -> tokenService.getTokenAssetInfo(999L));
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
        assertThat(result.get(0).getAllocationPerToken()).isEqualTo(500L);
    }

    @Test
    void getAllocationInfo_이벤트없음_빈리스트() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).totalSupply(1000L).asset(asset).build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(allocationEventRepository.findAllByAssetIdOrderBySettledAtDesc(1L)).thenReturn(List.of());

        assertThat(tokenService.getAllocationInfo(1L)).isEmpty();
    }

    @Test
    void getDisclosureInfo_정상조회() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).asset(asset).build();
        Disclosure disclosure = Disclosure.builder()
                .disclosureId(10L)
                .disclosureTitle("2024년 1분기 배당 공시")
                .disclosureContent("배당금 지급 안내")
                .disclosureCategory(DisclosureCategory.DIVIDEND)
                .assetId(1L)
                .build();
        File file = File.builder().fileId(100L).disclosureId(10L).originName("공시문서.pdf").build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findAllByAssetId(1L)).thenReturn(List.of(disclosure));
        when(fileRepository.findByDisclosureId(10L)).thenReturn(file);

        List<TokenDisclosureResponseDto> result = tokenService.getDisclosureInfo(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getDisclosureTitle()).isEqualTo("2024년 1분기 배당 공시");
        assertThat(result.get(0).getOriginName()).isEqualTo("공시문서.pdf");
    }

    @Test
    void getDisclosureInfo_파일없음_originNameNull() {
        Asset asset = Asset.builder().assetId(1L).build();
        Token token = Token.builder().tokenId(1L).asset(asset).build();
        Disclosure disclosure = Disclosure.builder()
                .disclosureId(10L)
                .disclosureTitle("건물 공시")
                .disclosureCategory(DisclosureCategory.BUILDING)
                .assetId(1L)
                .build();

        when(tokenRepository.findByIdWithAsset(1L)).thenReturn(Optional.of(token));
        when(disclosureRepository.findAllByAssetId(1L)).thenReturn(List.of(disclosure));
        when(fileRepository.findByDisclosureId(10L)).thenReturn(null);

        List<TokenDisclosureResponseDto> result = tokenService.getDisclosureInfo(1L);

        assertThat(result.get(0).getOriginName()).isNull();
    }

    @Test
    void getTokenAssetsWith10Paging_BASIC_DAY_정상조회() {
        Asset asset = Asset.builder().assetId(1L).assetName("서울 빌딩").build();
        Token token = Token.builder().tokenId(1L).currentPrice(12000L).asset(asset).build();
        CandleDay baseCandle = CandleDay.builder().closePrice(10000L).token(token).build();

        when(tokenRepository.findAllBySelectType(0, SelectType.BASIC)).thenReturn(List.of(token));
        when(candleDayRepository.findLatestBeforeByTokenIds(anyList(), any())).thenReturn(List.of(baseCandle));
        when(tradeRepository.findAggregatesByTokenIds(anyList(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{1L, 50000000L, 300L}));

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.BASIC, PeriodType.DAY);

        assertThat(result).hasSize(1);
        TokenMainResponseDto dto = result.get(0);
        assertThat(dto.getTokenId()).isEqualTo(1L);
        assertThat(dto.getAssetName()).isEqualTo("서울 빌딩");
        assertThat(dto.getCurrentPrice()).isEqualTo(12000L);
        assertThat(dto.getBasePrice()).isEqualTo(10000L);
        assertThat(dto.getTotalTradeValue()).isEqualTo(50000000L);
        assertThat(dto.getTotalTradeQuantity()).isEqualTo(300L);
        assertThat(dto.getFluctuationRate()).isEqualTo(20.0);
        assertThat(dto.getSparkLine()).isEmpty();
    }

    @Test
    void getTokenAssetsWith10Paging_토큰없음_빈리스트반환() {
        when(tokenRepository.findAllBySelectType(0, SelectType.BASIC)).thenReturn(List.of());

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.BASIC, PeriodType.DAY);

        assertThat(result).isEmpty();
        verifyNoInteractions(candleDayRepository, tradeRepository);
    }

    @Test
    void getTokenAssetsWith10Paging_currentPrice가Null이면_0으로처리() {
        Asset asset = Asset.builder().assetId(1L).assetName("null가격토큰").build();
        Token token = Token.builder().tokenId(1L).currentPrice(null).asset(asset).build();

        when(tokenRepository.findAllBySelectType(0, SelectType.BASIC)).thenReturn(List.of(token));
        when(candleDayRepository.findLatestBeforeByTokenIds(anyList(), any())).thenReturn(List.of());
        when(tradeRepository.findAggregatesByTokenIds(anyList(), any())).thenReturn(List.of());

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.BASIC, PeriodType.DAY);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCurrentPrice()).isEqualTo(0L);
        assertThat(result.get(0).getFluctuationRate()).isEqualTo(0.0);
    }

    @Test
    void getTokenAssetsWith10Paging_거래없는토큰_집계기본값() {
        Asset asset = Asset.builder().assetId(1L).assetName("무거래토큰").build();
        Token token = Token.builder().tokenId(1L).currentPrice(5000L).asset(asset).build();

        when(tokenRepository.findAllBySelectType(0, SelectType.TOTAL_TRADE_VALUE)).thenReturn(List.of(token));
        when(candleDayRepository.findLatestBeforeByTokenIds(anyList(), any())).thenReturn(List.of());
        when(tradeRepository.findAggregatesByTokenIds(anyList(), any())).thenReturn(List.of());

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.TOTAL_TRADE_VALUE, PeriodType.DAY);

        assertThat(result.get(0).getTotalTradeValue()).isEqualTo(0L);
        assertThat(result.get(0).getTotalTradeQuantity()).isEqualTo(0L);
    }

    @Test
    void getTokenAssetsWith10Paging_basePrice없으면_등락률0() {
        Asset asset = Asset.builder().assetId(1L).assetName("빌딩A").build();
        Token token = Token.builder().tokenId(1L).currentPrice(8000L).asset(asset).build();

        when(tokenRepository.findAllBySelectType(0, SelectType.BASIC)).thenReturn(List.of(token));
        when(candleDayRepository.findLatestBeforeByTokenIds(anyList(), any())).thenReturn(List.of());
        when(tradeRepository.findAggregatesByTokenIds(anyList(), any())).thenReturn(List.of());

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.BASIC, PeriodType.DAY);

        assertThat(result.get(0).getFluctuationRate()).isEqualTo(0.0);
    }

    @Test
    void getTokenAssetsWith10Paging_MONTH도_day기준BasePrice를사용() {
        Asset asset = Asset.builder().assetId(1L).assetName("월간 토큰").build();
        Token token = Token.builder().tokenId(1L).currentPrice(20000L).asset(asset).build();
        CandleDay baseCandle = CandleDay.builder().closePrice(18000L).token(token).build();

        when(tokenRepository.findAllBySelectType(0, SelectType.BASIC)).thenReturn(List.of(token));
        when(candleDayRepository.findLatestBeforeByTokenIds(anyList(), any())).thenReturn(List.of(baseCandle));
        when(tradeRepository.findAggregatesByTokenIds(anyList(), any())).thenReturn(List.of());

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.BASIC, PeriodType.MONTH);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getFluctuationRate())
                .isEqualTo(Math.round((20000.0 - 18000.0) / 18000.0 * 100.0 * 100.0) / 100.0);
        verifyNoInteractions(candleMonthRepository, candleYearRepository);
    }

    @Test
    void getTokenAssetsWith10Paging_YEAR도_day기준BasePrice를사용() {
        Asset asset = Asset.builder().assetId(1L).assetName("연간 토큰").build();
        Token token = Token.builder().tokenId(1L).currentPrice(30000L).asset(asset).build();
        CandleDay baseCandle = CandleDay.builder().closePrice(25000L).token(token).build();

        when(tokenRepository.findAllBySelectType(0, SelectType.BASIC)).thenReturn(List.of(token));
        when(candleDayRepository.findLatestBeforeByTokenIds(anyList(), any())).thenReturn(List.of(baseCandle));
        when(tradeRepository.findAggregatesByTokenIds(anyList(), any())).thenReturn(List.of());

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.BASIC, PeriodType.YEAR);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBasePrice()).isEqualTo(25000L);
        assertThat(result.get(0).getSparkLine()).isEmpty();
        verifyNoInteractions(candleMonthRepository, candleYearRepository);
    }

    @Test
    void getTokenAssetsWith10Paging_복수토큰_basePrice매핑() {
        Asset asset1 = Asset.builder().assetId(1L).assetName("A토큰").build();
        Asset asset2 = Asset.builder().assetId(2L).assetName("B토큰").build();
        Token token1 = Token.builder().tokenId(1L).currentPrice(1000L).asset(asset1).build();
        Token token2 = Token.builder().tokenId(2L).currentPrice(2000L).asset(asset2).build();
        CandleDay base1 = CandleDay.builder().closePrice(900L).token(token1).build();
        CandleDay base2 = CandleDay.builder().closePrice(1800L).token(token2).build();

        when(tokenRepository.findAllBySelectType(0, SelectType.BASIC)).thenReturn(List.of(token1, token2));
        when(candleDayRepository.findLatestBeforeByTokenIds(anyList(), any())).thenReturn(List.of(base1, base2));
        when(tradeRepository.findAggregatesByTokenIds(anyList(), any())).thenReturn(List.of());

        List<TokenMainResponseDto> result = tokenService.getTokenAssetsWith10Paging(0, SelectType.BASIC, PeriodType.DAY);

        TokenMainResponseDto dto1 = result.stream().filter(d -> d.getTokenId() == 1L).findFirst().orElseThrow();
        TokenMainResponseDto dto2 = result.stream().filter(d -> d.getTokenId() == 2L).findFirst().orElseThrow();

        assertThat(dto1.getBasePrice()).isEqualTo(900L);
        assertThat(dto2.getBasePrice()).isEqualTo(1800L);
        assertThat(dto1.getSparkLine()).isEmpty();
        assertThat(dto2.getSparkLine()).isEmpty();
    }
}

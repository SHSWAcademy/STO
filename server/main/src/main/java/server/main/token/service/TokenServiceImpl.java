package server.main.token.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.allocation.entity.AllocationEvent;
import server.main.allocation.repository.AllocationEventRepository;
import server.main.candle.dto.LiveCandleDto;
import server.main.candle.entity.Candle;
import server.main.candle.entity.CandleDay;
import server.main.candle.repository.CandleDayRepository;
import server.main.candle.service.CandleLiveManager;
import server.main.candle.repository.CandleMonthRepository;
import server.main.candle.repository.CandleYearRepository;
import server.main.token.dto.SelectType;
import server.main.asset.entity.Asset;
import server.main.disclosure.entity.Disclosure;
import server.main.disclosure.repository.DisclosureRepository;
import server.main.global.error.BusinessException;
import server.main.global.util.TickSizePolicy;
import static server.main.global.error.ErrorCode.TOKEN_NOT_FOUND;
import server.main.global.file.File;
import server.main.global.file.FileRepository;
import server.main.token.dto.*;
import server.main.token.entity.Token;
import server.main.token.mapper.TokenMapper;
import server.main.token.repository.TokenRepository;
import server.main.global.util.GeminiClient;
import server.main.trade.entity.Trade;
import server.main.trade.repository.TradeRepository;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static server.main.global.error.ErrorCode.ENTITY_NOT_FOUNT_ERROR;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService{

    private final DisclosureRepository disclosureRepository;
    private final CandleLiveManager candleLiveManager;
    private final CandleDayRepository candleDayRepository;
    private final CandleMonthRepository candleMonthRepository;
    private final CandleYearRepository candleYearRepository;
    private final FileRepository fileRepository;
    private final TokenRepository tokenRepository;
    private final AllocationEventRepository allocationEventRepository;
    private final TradeRepository tradeRepository;
    private final TokenMapper tokenMapper;
    private final GeminiClient geminiClient;


    // 토큰 상세 페이지 - 차트, 호가 데이터
    @Override
    public TokenChartDetailResponseDto getTokenDetail(Long tokenId) {

        // 토큰, 자산 데이터 세팅
        Token findToken = tokenRepository.findByIdWithAsset(tokenId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        TokenChartDetailResponseDto dto = tokenMapper.toDtoDetail(findToken);

        // 전날 종가 — 현재 일봉 버킷 시작 이전 캔들만 조회
        LocalDateTime startOfToday = getDayBucket(LocalDateTime.now());
        Long yesterdayClosePrice = candleDayRepository.findLatestBefore(tokenId, startOfToday)
                .map(CandleDay::getClosePrice)
                .orElse(null);

        dto.setYesterdayClosePrice(yesterdayClosePrice);

        // 호가창에 보여줄 데이터 : 오늘 시가, 최고가, 최저가 — DB가 아닌 메모리(liveDay)에서 조회
        LiveCandleDto liveDay = candleLiveManager.getLiveDay(tokenId);
        if (liveDay != null) {
            dto.setTodayOpenPrice(liveDay.getOpenPrice());
            dto.setTodayHighPrice(liveDay.getHighPrice());
            dto.setTodayLowPrice(liveDay.getLowPrice());
        }

        return dto;
    }

    // 토큰 상세 페이지 - 종목 정보 데이터
    @Override
    public TokenAssetInfoResponseDto getTokenAssetInfo(Long tokenId) {
        // token, asset 조회
        Token token = tokenRepository.findByIdWithAsset(tokenId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Asset asset = token.getAsset();

        String originName = disclosureRepository
                .findByAssetIdAndCategory(asset.getAssetId())
                .map(disclosure -> fileRepository.findByDisclosureId(disclosure.getDisclosureId()))
                .map(File::getOriginName)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        return TokenAssetInfoResponseDto.builder()
                .initPrice(asset.getInitPrice())
                .totalValue(asset.getTotalValue())
                .assetAddress(asset.getAssetAddress())
                .originName(originName)
                .totalSupply(asset.getTotalSupply())
                .createdAt(asset.getCreatedAt())
                .build();
    }

    @Override
    public List<TokenAllocationInfoResponseDto> getAllocationInfo(Long tokenId) {
        // 상세 페이지 -> 배당금 내역
        Token token = tokenRepository.findByIdWithAsset(tokenId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        Asset asset = token.getAsset();
        Long assetId = asset.getAssetId();


        List<AllocationEvent> findEvents = allocationEventRepository.findAllByAssetIdOrderBySettledAtDesc(assetId); // 역순
        List<TokenAllocationInfoResponseDto> dtos = new ArrayList<>();
        Long totalSupply = token.getTotalSupply();

        for (AllocationEvent a : findEvents) {
            Long perToken = (totalSupply != null && totalSupply > 0) ? a.getMonthlyDividendIncome() / totalSupply : 0L;

            TokenAllocationInfoResponseDto dto = TokenAllocationInfoResponseDto.builder()
                    .settledAt(a.getSettledAt())
                    .monthlyDividendIncome(a.getMonthlyDividendIncome())
                    .allocationPerToken(perToken)
                    .allocationBatchStatus(a.getAllocationBatchStatus())
                    .build();
            dtos.add(dto);
        }
        return dtos;
    }

    @Override
    public List<TokenDisclosureResponseDto> getDisclosureInfo(Long tokenId) {
        Token findToken = tokenRepository.findByIdWithAsset(tokenId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Long assetId = findToken.getAsset().getAssetId();

        List<Disclosure> disclosures = disclosureRepository.findAllByAssetId(assetId);

        // dto 리턴
        return disclosures.stream()
                .map(d -> {
                    File file = fileRepository.findByDisclosureId(d.getDisclosureId());
                    return TokenDisclosureResponseDto.builder()
                            .disclosureTitle(d.getDisclosureTitle())
                            .disclosureContent(d.getDisclosureContent())
                            .disclosureCategory(d.getDisclosureCategory())           // 공시 카테고리 - BUILDING, DIVIDEND, ETC
                            .OriginName(file != null ? file.getOriginName() : null) // 상세페이지 공시 pdf 파일은 null일 수 있다
                            .createdAt(d.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<TokenMainResponseDto> getTokenAssetsWith10Paging(int page, SelectType selectType, PeriodType periodType) {

        // 1. 토큰 조회 : 정렬 기준에 따라 페이징된 Token 목록 조회
        // selectType : 기본값 전체, 사용자가 메인 페이지에서 거래 대금, 거래량 선택 시 해당 필드로 정렬해서 가져온다
        List<Token> tokens = tokenRepository.findAllBySelectType(page, selectType);
        if (tokens.isEmpty()) return List.of();

        // 2. 토큰 id 추출
        List<Long> tokenIds = tokens.stream().map(Token::getTokenId).toList();

        // 3. 토큰 별 1일 (1개월, 1년) 시가 조회, Map<tokenId, openPrice> : key 토큰 id, value 시가 (등락률 계산에 필요)
        Map<Long, Long> basePriceMap = getBasePriceMap(tokenIds, periodType);

        // 4. 토큰 id 별 이때 동안의 전체 거래 대금, 전체 거래량 조회
        LocalDateTime todayStart = getDayBucket(LocalDateTime.now());
        Map<Long, long[]> tradeAggMap = tradeRepository.findAggregatesByTokenIds(tokenIds, todayStart)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new long[]{ ((Number) row[1]).longValue(), ((Number) row[2]).longValue() }
                ));


        // dto 로 만들어서 전달
        return tokens.stream().map(t -> {
            Long tokenId = t.getTokenId();
            Long currentPrice = t.getCurrentPrice() != null ? t.getCurrentPrice() : 0L;
            Long basePrice = basePriceMap.get(tokenId);
            long[] agg = tradeAggMap.getOrDefault(tokenId, new long[]{0L, 0L});

            double fluctuationRate = (basePrice != null && basePrice > 0)
                    ? ((double) (currentPrice - basePrice) / basePrice) * 100
                    : 0.0;

            return TokenMainResponseDto.builder()
                    .tokenId(tokenId)
                    .assetName(t.getAsset().getAssetName())
                    .basePrice(basePrice)
                    .currentPrice(currentPrice)
                    .fluctuationRate(Math.round(fluctuationRate * 100.0) / 100.0)
                    .totalTradeValue(agg[0])
                    .totalTradeQuantity(agg[1])
                    .sparkLine(List.of())
                    .tokenSymbol(t.getTokenSymbol())
                    .imgUrl(t.getAsset().getImgUrl())
                    .aiSummary(t.getAiSummary())
                    .aiSummaryUpdatedAt(t.getAiSummaryUpdatedAt())
                    .totalSupply(t.getTotalSupply())
                    .circulatingSupply(t.getCirculatingSupply())
                    .initPrice(t.getInitPrice())
                    .build();
        }).collect(Collectors.toList());
    }

    // 토큰 id, 해당 캔들의 최근 7일 (월, 년) 종가 리스트 - 스파크 라인 전용
    private Map<Long, List<SparkPointDto>> getSparklineMap(List<Long> tokenIds, PeriodType periodType) {
        LocalDateTime now = LocalDateTime.now();

        // 당일(당월/당해) 제외: before = 현재 기간의 시작 시각
        LocalDateTime before = switch (periodType) {
            case DAY   -> getDayBucket(now);
            case MONTH -> now.toLocalDate().withDayOfMonth(1).atStartOfDay();
            case YEAR  -> now.toLocalDate().withDayOfYear(1).atStartOfDay();
        };

        // before 기준으로 7개 이전 기간
        LocalDateTime since = switch (periodType) {
            case DAY   -> before.minusDays(7);
            case MONTH -> before.minusMonths(7);
            case YEAR  -> before.minusYears(7);
        };

        List<? extends Candle> candles = switch (periodType) {
            case DAY   -> candleDayRepository.findRecentByTokenIds(tokenIds, since, before);
            case MONTH -> candleMonthRepository.findRecentByTokenIds(tokenIds, since, before);
            case YEAR  -> candleYearRepository.findRecentByTokenIds(tokenIds, since, before);
        };

        // 캔들 리스트 -> Map<Long, List<Long>> 리턴 (토큰 자산 id, 캔들 종가 리스트)
        DateTimeFormatter formatter = switch (periodType) {
            case DAY   -> DateTimeFormatter.ofPattern("MM.dd");
            case MONTH -> DateTimeFormatter.ofPattern("yy.MM");
            case YEAR  -> DateTimeFormatter.ofPattern("yyyy");
        };

        return candles.stream().collect(Collectors.groupingBy(
                c -> c.getToken().getTokenId(),
                Collectors.mapping(
                        c -> new SparkPointDto(c.getClosePrice(), c.getCandleTime().format(formatter)),
                        Collectors.toList()
                )
        ));
    }


    // 등락률 기준가 — 기간과 무관하게 항상 전일 종가(09:00 버킷 이전 마지막 일봉)
    private Map<Long, Long> getBasePriceMap(List<Long> tokenIds, PeriodType periodType) {
        LocalDateTime startOfToday = getDayBucket(LocalDateTime.now());
        return candleDayRepository.findLatestBeforeByTokenIds(tokenIds, startOfToday)
                .stream().collect(Collectors.toMap(
                        c -> c.getToken().getTokenId(),
                        c -> c.getClosePrice(),
                        (a, b) -> a
                ));
    }


    private LocalDateTime getDayBucket(LocalDateTime now) {
        return now.getHour() >= 9
                ? now.toLocalDate().atTime(9, 0)
                : now.toLocalDate().minusDays(1).atTime(9, 0);
    }

    @Override
    public Map<Long, List<SparkPointDto>> getSparklines(List<Long> tokenIds, PeriodType periodType) {
        if (tokenIds == null || tokenIds.isEmpty()) return Map.of();
        return getSparklineMap(tokenIds, periodType);
    }

    @Override
    public TokenSummaryResponseDto getMarketSummary() {
        List<Token> allTokens = tokenRepository.findAllTradingTokensWithAsset();
        if (allTokens.isEmpty()) {
            return TokenSummaryResponseDto.builder()
                    .totalAssets(0).totalMarketCap(0L).todayTradeValue(0L)
                    .upCount(0).downCount(0).topUp(List.of()).topDown(List.of())
                    .build();
        }

        List<Long> tokenIds = allTokens.stream().map(Token::getTokenId).toList();

        long totalMarketCap = allTokens.stream()
                .mapToLong(t -> {
                    long price = t.getCurrentPrice() != null ? t.getCurrentPrice() : 0L;
                    long supply = t.getTotalSupply() != null ? t.getTotalSupply() : 0L;
                    return price * supply;
                })
                .sum();

        LocalDateTime todayStart = getDayBucket(LocalDateTime.now());
        long todayTradeValue = tradeRepository.sumAllTodayTradeValue(todayStart);

        Map<Long, Long> basePriceMap = getBasePriceMap(tokenIds, PeriodType.DAY);

        List<TopMoverDto> allMovers = allTokens.stream()
                .map(t -> {
                    Long basePrice = basePriceMap.get(t.getTokenId());
                    Long currentPrice = t.getCurrentPrice() != null ? t.getCurrentPrice() : 0L;
                    double rate = (basePrice != null && basePrice > 0)
                            ? Math.round(((double) (currentPrice - basePrice) / basePrice) * 100 * 100.0) / 100.0
                            : 0.0;
                    return new TopMoverDto(t.getTokenId(), t.getAsset().getAssetName(), t.getTokenSymbol(), rate, currentPrice, t.getAsset().getImgUrl());
                })
                .sorted((a, b) -> Double.compare(b.getFluctuationRate(), a.getFluctuationRate()))
                .collect(Collectors.toList());

        int upCount = (int) allMovers.stream().filter(m -> m.getFluctuationRate() > 0).count();
        int downCount = (int) allMovers.stream().filter(m -> m.getFluctuationRate() < 0).count();

        List<TopMoverDto> topUp = allMovers.stream()
                .filter(m -> m.getFluctuationRate() > 0)
                .limit(3)
                .collect(Collectors.toList());

        List<TopMoverDto> topDown = allMovers.stream()
                .filter(m -> m.getFluctuationRate() < 0)
                .sorted((a, b) -> Double.compare(a.getFluctuationRate(), b.getFluctuationRate()))
                .limit(3)
                .collect(Collectors.toList());

        return TokenSummaryResponseDto.builder()
                .totalAssets(allTokens.size())
                .totalMarketCap(totalMarketCap)
                .todayTradeValue(todayTradeValue)
                .upCount(upCount)
                .downCount(downCount)
                .topUp(topUp)
                .topDown(topDown)
                .build();
    }

    @Override
    @Cacheable(value = "tokenAssetName", key = "#p0")
    public String getAssetName(Long tokenId) {
        return tokenRepository.findByIdWithAsset(tokenId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR))
                .getAsset()
                .getAssetName();
    }

    @Override
    public long getTickSize(Long tokenId) {
        Token findToken = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(TOKEN_NOT_FOUND));
        Long currentPrice = findToken.getCurrentPrice() != null
                ? findToken.getCurrentPrice()
                : findToken.getInitPrice();
        return TickSizePolicy.getTickSize(currentPrice);
    }
}

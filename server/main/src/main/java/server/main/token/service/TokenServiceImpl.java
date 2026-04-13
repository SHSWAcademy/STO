package server.main.token.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.allocation.entity.AllocationEvent;
import server.main.allocation.repository.AllocationEventRepository;
import server.main.candle.entity.Candle;
import server.main.candle.repository.CandleDayRepository;
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
import server.main.trade.repository.TradeRepository;

import java.time.LocalDateTime;
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
    private final CandleDayRepository candleDayRepository;
    private final CandleMonthRepository candleMonthRepository;
    private final CandleYearRepository candleYearRepository;
    private final FileRepository fileRepository;
    private final TokenRepository tokenRepository;
    private final AllocationEventRepository allocationEventRepository;
    private final TradeRepository tradeRepository;
    private final TokenMapper tokenMapper;


    // 토큰 상세 페이지 - 차트, 호가 데이터
    @Override
    public TokenChartDetailResponseDto getTokenDetail(Long tokenId) {

        // 토큰, 자산 데이터 세팅
        Token findToken = tokenRepository.findByIdWithAsset(tokenId)
                .orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        TokenChartDetailResponseDto dto = tokenMapper.toDtoDetail(findToken);

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

        // 4. 스파크라인 조회 - 토큰 id 별 최근 7일 (월, 년) 종가 : Map<tokenId, List<closePrice>> (스파크 라인에 필요)
        Map<Long, List<Long>> sparklineMap = getSparklineMap(tokenIds, periodType);

        // 5. 토큰 id 별 이때 동안의 전체 거래 대금, 전체 거래량 조회
        Map<Long, long[]> tradeAggMap = tradeRepository.findAggregatesByTokenIds(tokenIds)
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

            double fluctuationRate = (basePrice != null && basePrice > 0) ? (currentPrice - basePrice) / basePrice * 100 : 0.0;

            return TokenMainResponseDto.builder()
                    .tokenId(tokenId)
                    .assetName(t.getAsset().getAssetName())
                    .currentPrice(currentPrice)
                    .fluctuationRate(Math.round(fluctuationRate * 100.0) / 100.0)
                    .totalTradeValue(agg[0])
                    .totalTradeQuantity(agg[1])
                    .sparkLine(sparklineMap.getOrDefault(tokenId, List.of()))
                    .build();
        }).collect(Collectors.toList());
    }

    // 토큰 id, 해당 캔들의 최근 7일 (월, 년) 종가 리스트 - 스파크 라인 전용
    private Map<Long, List<Long>> getSparklineMap(List<Long> tokenIds, PeriodType periodType) {
        LocalDateTime since = switch (periodType) {
            case DAY   -> LocalDateTime.now().minusDays(7);   // 최근 7일치 조회를 위한 시작일
            case MONTH -> LocalDateTime.now().minusMonths(7); // 최근 7달치 조회를 위한 시작 달
            case YEAR  -> LocalDateTime.now().minusYears(7);  // 최근 7년치 조회를 위한 시작 년도
        };

        // day 일 경우   : 각 토큰 자산들의 최근 7일치 candles 데이터 리스트로 조회, candleDay 리스트 리턴
        // month 일 경우 : 각 토큰 자산들의 최근 7개월치 candles 데이터 리스트로 조회, candleMonth 리스트 리턴
        // year 일 경우  : 각 토큰 자산들의 최근 7년치 candles 데이터 리스트로 조회, candleYear 리스트 리턴
        List<? extends Candle> candles = switch (periodType) {
            case DAY   -> candleDayRepository.findRecentByTokenIds(tokenIds, since);
            case MONTH -> candleMonthRepository.findRecentByTokenIds(tokenIds, since);
            case YEAR  -> candleYearRepository.findRecentByTokenIds(tokenIds, since);
        };

        // 캔들 리스트 -> Map<Long, List<Long>> 리턴 (토큰 자산 id, 캔들 종가 리스트)
        return candles.stream().collect(Collectors.groupingBy(
                c -> c.getToken().getTokenId(),
                Collectors.mapping(c -> c.getClosePrice(), Collectors.toList())
        ));
    }


    // 토큰 id 별로 파라미터로 받은 기간의 (일, 월, 년) 시작가 조회 (key : tokenId, value : 시가) - 등락률 계산 전용
    private Map<Long, Long> getBasePriceMap(List<Long> tokenIds, PeriodType periodType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (periodType) {
            case DAY -> {
                LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
                LocalDateTime endOfDay   = startOfDay.plusDays(1);
                yield candleDayRepository.findTodayByTokenIds(tokenIds, startOfDay, endOfDay)
                        .stream().collect(Collectors.toMap(
                                c -> c.getToken().getTokenId(),
                                c -> c.getOpenPrice(),
                                (a, b) -> a
                        ));
            }
            case MONTH -> {
                LocalDateTime startOfMonth = now.toLocalDate().withDayOfMonth(1).atStartOfDay();
                LocalDateTime endOfMonth   = startOfMonth.plusMonths(1);
                yield candleMonthRepository.findThisMonthByTokenIds(tokenIds, startOfMonth, endOfMonth)
                        .stream().collect(Collectors.toMap(
                                c -> c.getToken().getTokenId(),
                                c -> c.getOpenPrice(),
                                (a, b) -> a
                        ));
            }
            case YEAR -> {
                LocalDateTime startOfYear = now.toLocalDate().withDayOfYear(1).atStartOfDay();
                LocalDateTime endOfYear   = startOfYear.plusYears(1);
                yield candleYearRepository.findThisYearByTokenIds(tokenIds, startOfYear, endOfYear)
                        .stream().collect(Collectors.toMap(
                                c -> c.getToken().getTokenId(),
                                c -> c.getOpenPrice(),
                                (a, b) -> a
                        ));
            }
        };
    }

    @Override
    public long getTickSize(Long tokenId) {
        Token findToken = tokenRepository.findById(tokenId)
                .orElseThrow(() -> new BusinessException(TOKEN_NOT_FOUND));
        return TickSizePolicy.getTickSize(findToken.getCurrentPrice());
    }
}

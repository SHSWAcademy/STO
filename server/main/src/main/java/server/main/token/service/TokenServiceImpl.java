package server.main.token.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.allocation.entity.AllocationEvent;
import server.main.allocation.repository.AllocationEventRepository;
import server.main.candle.repository.CandleDayRepository;
import server.main.candle.repository.CandleMonthRepository;
import server.main.candle.repository.CandleYearRepository;
import server.main.token.dto.SelectType;
import server.main.asset.entity.Asset;
import server.main.disclosure.entity.Disclosure;
import server.main.disclosure.repository.DisclosureRepository;
import server.main.global.error.BusinessException;
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

        // 정렬 기준에 따라 페이징된 Token 목록 조회
        // selectType : 기본값 전체, 사용자가 메인 페이지에서 거래 대금, 거래량 선택 시 해당 필드로 정렬해서 가져온다
        List<Token> tokens = tokenRepository.findAllBySelectType(page, selectType);
        if (tokens.isEmpty()) return List.of();

        List<Long> tokenIds = tokens.stream().map(Token::getTokenId).toList();

        // 기간별(1일, 1개월, 1년) base price(시가) 조회, Map<tokenId, openPrice>
        Map<Long, Double> basePriceMap = getBasePriceMap(tokenIds, periodType);

        // 거래 집계 조회, Map<tokenId, [totalValue, totalQty]>
        Map<Long, long[]> tradeAggMap = tradeRepository.findAggregatesByTokenIds(tokenIds)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> new long[]{ ((Number) row[1]).longValue(), ((Number) row[2]).longValue() }
                ));

        // 스파크라인 조회, Map<tokenId, List<closePrice>>
        Map<Long, List<Long>> sparklineMap = getSparklineMap(tokenIds, periodType);

        // dto 로 만들어서 전달
        return tokens.stream().map(t -> {
            Long tokenId = t.getTokenId();
            double currentPrice = t.getCurrentPrice() != null ? t.getCurrentPrice() : 0.0;
            Double basePrice = basePriceMap.get(tokenId);
            long[] agg = tradeAggMap.getOrDefault(tokenId, new long[]{0L, 0L});

            double fluctuationRate = (basePrice != null && basePrice > 0) ? (currentPrice - basePrice) / basePrice * 100 : 0.0;

            return TokenMainResponseDto.builder()
                    .tokenId(tokenId)
                    .assetName(t.getAsset().getAssetName())
                    .currentPrice((long) currentPrice)
                    .fluctuationRate(Math.round(fluctuationRate * 100.0) / 100.0)
                    .totalTradeValue(agg[0])
                    .totalTradeQuantity(agg[1])
                    .sparkLine(sparklineMap.getOrDefault(tokenId, List.of()))
                    .build();
        }).collect(Collectors.toList());
    }

    private Map<Long, List<Long>> getSparklineMap(List<Long> tokenIds, PeriodType periodType) {
        LocalDateTime since = switch (periodType) {
            case DAY   -> LocalDateTime.now().minusDays(7);
            case MONTH -> LocalDateTime.now().minusMonths(7);
            case YEAR  -> LocalDateTime.now().minusYears(7);
        };

        var candles = switch (periodType) {
            case DAY   -> candleDayRepository.findRecentByTokenIds(tokenIds, since);
            case MONTH -> candleMonthRepository.findRecentByTokenIds(tokenIds, since);
            case YEAR  -> candleYearRepository.findRecentByTokenIds(tokenIds, since);
        };

        return candles.stream().collect(Collectors.groupingBy(
                c -> c.getToken().getTokenId(),
                Collectors.mapping(c -> c.getClosePrice().longValue(), Collectors.toList())
        ));
    }

    private Map<Long, Double> getBasePriceMap(List<Long> tokenIds, PeriodType periodType) {
        LocalDateTime now = LocalDateTime.now();
        return switch (periodType) {
            // A 자산의 시작가, B 자산의 시작가, C 자산의 시작가 들이 들어간다.
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
}

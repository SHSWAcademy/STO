package server.main.token.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.asset.entity.Asset;
import server.main.asset.mapper.AssetMapper;
import server.main.asset.repository.AssetRepository;
import server.main.diclosure.entity.Disclosure;
import server.main.diclosure.repository.DisclosureRepository;
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

import java.util.List;
import java.util.stream.Collectors;

import static server.main.global.error.ErrorCode.ENTITY_NOT_FOUNT_ERROR;

@Transactional(readOnly = true)
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenServiceImpl implements TokenService{

    private final DisclosureRepository disclosureRepository;
    private final FileRepository fileRepository;
    private final TokenRepository tokenRepository;
    private final AllocationEventRepository allocationEventRepository;
    private final AllocationPayoutRepository allocationPayoutRepository;
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
        Token token = tokenRepository.findById(tokenId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Asset asset = token.getAsset();

        String originName = disclosureRepository
                .findByAssetIdAndCategory(asset.getAssetId())
                .map(disclosure -> fileRepository.findByDisclosureId(disclosure.getDisclosureId()))
                .map(File::getOrigin_name)
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
        //    // 상세 페이지 -> 배당금 내역
        //    // 필요 테이블 : ALLOCATION_EVENTS, ALLOCATION_PAYOUTS, TOKENS
        //    private LocalDateTime settledAt;    // 배당 지급일 (ALLOCATION_EVENTS 테이블)
        //    private int allocationPerToken;     // 주당 배당금 (토큰 a의 배당 월 수익 / 토큰 전체 수) -> ALLOCATION_EVENT, TOKENS 테이블, 별도 컬럼 필요 ?, int로 해도 되겠죠 ..?
        //    private Long monthlyDividendIncome; // 총 배당금 (ALLOCATION_EVENTS 테이블)
        //    private AllocationPayoutStatus status; // 지급 상태 (대기, 성공, 실패) -> ALLOCATION_PAYOUTS 테이블
        Token token = tokenRepository.findById(tokenId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Asset asset = token.getAsset();
        Long assetId = asset.getAssetId();

        AllocationEvent findEvent = allocationEventRepository.findBy
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
                            .OriginName(file != null ? file.getOrigin_name() : null) // 상세페이지 공시 pdf 파일은 null일 수 있다
                            .createdAt(d.getCreatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }
}

package server.main.asset.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.asset.dto.AssetMainResponseDto;
import server.main.asset.entity.*;
import server.main.asset.repository.AssetAccountRepository;
import server.main.asset.repository.AssetBankingRepository;
import server.main.asset.repository.AssetRepository;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.token.entity.Token;

import java.util.List;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AssetServiceImpl implements AssetService{
    private final AssetRepository assetRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final AssetBankingRepository assetBankingRepository;

    // 자산등록 (admin)
    @Transactional
    @Override
    public Asset registerAsset(Asset asset) {
        return assetRepository.save(asset);
    }
    // 기존 자산조회 (admin)
    @Override
    public Asset findById(Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
    }

    // 자산 이름조회 (admin)
    @Override
    public String findAssetName(Long assetId) {
        return assetRepository.findAssetName(assetId);
    }

    // 자산 첫 등록 시 계좌 생성 (admin)
    @Transactional
    @Override
    public void registerAssetAccount(Token token) {
        // 계좌 먼저 생성
        AssetAccount saveAccount = AssetAccount.builder()
                .assetId(token.getAsset().getAssetId())
                .build();
        log.info("계좌 생성 내역 : {}", saveAccount);

        // 계좌 저장
        AssetAccount account = assetAccountRepository.save(saveAccount);
        // 초기 금액 입금처리 (플랫폼 소유토큰 제외)
        AssetBanking assetBanking = AssetBanking.builder()
                // 토큰발행 (총 개수 - 실제 개수) * 초기가격 = 플랫폼 소유 가격만 입금함
                .assetBankingAmount((token.getTotalSupply() - token.getCirculatingSupply()) * token.getInitPrice())
                .assetAccount(account)
                .direction(AssetBankingDirection.DEPOSIT)
                .type(AssetBankingType.EXTRA)
                .build();
        log.info("초기 입금 내역 : {}", assetBanking);
        // 초기 금액 입금처리
        AssetBanking depositExtra = assetBankingRepository.save(assetBanking);
        // 입금내역 잔고 업데이트
        account.deposit(depositExtra.getAssetBankingAmount());

        /*
          원래 시나리오상 공모를 진행하여 유저들이 구매를해야 출금이 되지만
          본 프로젝트에서는 공모를 진행했다고 가정하여 유저들에게 랜덤으로 출력할예정
        */

    }

    // 배당 월수익 입금처리
    @Transactional
    @Override
    public void depositAllocationAmount(Long amount, Long assetId) {
        // 입금 금액 검증
        if (amount == null || amount <= 0) {
            throw new BusinessException(ErrorCode.INVALID_AMOUNT);
        }
        // 계좌 조회후 null 검증
        AssetAccount assetAccount = assetAccountRepository.findByAssetId(assetId);
        if (assetAccount == null) {
            throw new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR);
        }

        // 계좌 입금처리
        AssetBanking assetBanking = AssetBanking.builder()
                .assetAccount(assetAccount)
                .assetBankingAmount(amount)
                .direction(AssetBankingDirection.DEPOSIT)
                .type(AssetBankingType.ALLOCATION)
                .build();
        // 입금내역 저장
        assetBankingRepository.save(assetBanking);
        // 계좌 입출금 가능액 업데이트
        assetAccount.deposit(amount);
    }

    @Override
    public List<AssetMainResponseDto> getAssetsWith10Paging(int page) {
        Pageable pageable = PageRequest.of(page, 10);
        return assetRepository.findAll(pageable).stream()
                .map(AssetMainResponseDto :: toMainDto)
                .collect(Collectors.toList());
    }
}

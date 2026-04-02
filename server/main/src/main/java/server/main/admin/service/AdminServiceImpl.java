package server.main.admin.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
//import server.main.admin.dto.AssetDetailResponseDTO;
//import server.main.admin.dto.AssetListResponseDTO;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.admin.mapper.AdminMapper;
import server.main.admin.repository.PlatformTokenHoldingsRepository;
import server.main.asset.entity.Asset;
import server.main.asset.repository.AssetRepository;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class AdminServiceImpl { // implements AdminService {
    private final PlatformTokenHoldingsRepository platformTokenHoldingsRepository;
    private final AssetRepository assetRepository;
    private final TokenRepository tokenRepository;
    private final AdminMapper adminMapper;

//    // 자산등록
//    // 자산 등록 -> 토큰 등록 -> 플랫폼 소유 토큰 등록
//    @Transactional
//    @Override
//    public void registerAsset(AssetRegisterRequestDTO dto) {
//
//        // 자산 정보 먼저 등록
//        Asset saveAsset = assetRepository.save(adminMapper.toAsset(dto));
//        log.info("부동산 저장 : {} ",saveAsset);
//
//        // 자산ID도 토큰 엔터티에 넣기
//        Token token = adminMapper.toToken(dto, saveAsset);
//        log.info("토큰 테이블 저장 : {} ", token);
//
//        // 토큰 테이블 SAVE
//        Token saveToken = tokenRepository.save(token);
//
//        // 플랫폼 보유 토큰 설정
//        PlatformTokenHolding platformTokenHoldings = adminMapper.toPlatformTokenHoldings(dto, saveToken);
//
//        // 플랫폼 보유 테이블 SAVE
//        platformTokenHoldingsRepository.save(platformTokenHoldings);
//    }
//
//

}
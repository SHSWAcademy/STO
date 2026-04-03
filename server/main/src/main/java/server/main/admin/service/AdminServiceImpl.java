package server.main.admin.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.AssetDetailResponseDTO;
import server.main.admin.dto.AssetListResponseDTO;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.admin.mapper.AdminMapper;
import server.main.admin.repository.PlatformTokenHoldingsRepository;
import server.main.asset.entity.Asset;
import server.main.asset.repository.AssetRepository;
import server.main.diclosure.service.DisclosureService;
import server.main.global.file.FileService;
import server.main.global.file.FileStore;
import server.main.notice.service.NoticeService;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class AdminServiceImpl implements AdminService {
    private final PlatformTokenHoldingsRepository platformTokenHoldingsRepository;
    private final AssetRepository assetRepository;
    private final TokenRepository tokenRepository;
    private final AdminMapper adminMapper;
    private final NoticeService noticeService;
    private final DisclosureService disclosureService;
    private final FileStore fileStore;
    private final FileService fileService;

    // 자산등록
    // 자산 등록 -> 건물이미지 등록 -> 토큰 등록 -> 플랫폼 소유 토큰 등록 -> 공시 / 공지 등록 -> 첨부파일 등록
    @Transactional
    @Override
    public void registerAsset(AssetRegisterRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile) {

        // 이미지 파일 디스크 저장 (DB 작업 전에 먼저 수행)
        String storedImageName = saveImage(imageFile);

        try {
            // 자산 정보 먼저 등록
            Asset saveAsset = assetRepository.save(adminMapper.toAsset(dto, storedImageName));
            log.info("부동산 저장 : {} ", saveAsset);

            // 자산ID도 토큰 엔터티에 넣기
            Token token = adminMapper.toToken(dto, saveAsset);
            log.info("토큰 테이블 저장 : {} ", token);
            // 토큰 테이블 SAVE
            Token saveToken = tokenRepository.save(token);

            // 플랫폼 보유 토큰 설정
            PlatformTokenHolding platformTokenHoldings = adminMapper.toPlatformTokenHoldings(dto, saveToken);
            log.info("플랫폼 보유 토큰 저장 : {}", platformTokenHoldings);
            // 플랫폼 보유 테이블 SAVE
            platformTokenHoldingsRepository.save(platformTokenHoldings);

            // 공지 등록 메서드 호출
            noticeService.registerAssetNotice(dto);

            // 공시 등록 메서드 호출
            String assetName = saveAsset.getAssetName();
            Long assetId = saveAsset.getAssetId();
            Long disclosureId = disclosureService.registerAssetDisclosure(assetName, assetId);

            // PDF파일 저장 (pdf 파일, 공시ID 넣어서 호출)
            fileService.savePdf(pdfFile, disclosureId);

        } catch (Exception e) {
            // DB 저장 실패 시 디스크에 저장된 이미지 파일 삭제
            deleteFileIfExists(storedImageName);
            throw e;
        }
    }

    // 이미지 저장
    private String saveImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) return null;
        try {
            return fileStore.saveFile(imageFile);
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일 저장 실패", e);
        }
    }

    // 예외 발생 시 파일 삭제처리
    private void deleteFileIfExists(String storedName) {
        if (storedName == null) return;
        new java.io.File(fileStore.getUploadDir(), storedName).delete();
    }

    // 자산 상세조회
    @Override
    public AssetDetailResponseDTO getAssetDetail(Long assetId) {
        PlatformTokenHolding holding = platformTokenHoldingsRepository.findWithTokenAndAssetByAssetId(assetId)
                .orElseThrow(() -> new EntityNotFoundException("자산을 찾을 수 없음"));
        return adminMapper.toAssetDetailResponseDTO(holding);
    }

    // 자산 리스트 조회
    @Override
    public List<AssetListResponseDTO> getAssetList() {
        return tokenRepository.findAllTokensWithAsset()
                .stream()
                .map(token -> adminMapper.toAssetListResponseDTO(token))
                .collect(Collectors.toList());
    }

}

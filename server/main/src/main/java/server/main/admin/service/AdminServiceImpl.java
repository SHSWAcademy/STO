package server.main.admin.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.*;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.admin.mapper.AdminMapper;
import server.main.admin.repository.PlatformTokenHoldingsRepository;
import server.main.allocation.entity.AllocationEvent;
import server.main.allocation.repository.AllocationEventRepository;
import server.main.asset.entity.Asset;
import server.main.asset.repository.AssetRepository;
import server.main.diclosure.service.DisclosureService;
import server.main.global.file.FileService;
import server.main.notice.service.NoticeService;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.util.List;
import java.util.Map;
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
    private final FileService fileService;
    private final AllocationEventRepository allocationEventRepository;

    // 자산등록
    // 자산 등록 -> 건물이미지 등록 -> 토큰 등록 -> 플랫폼 소유 토큰 등록 -> 공시 / 공지 등록 -> 첨부파일 등록
    @Transactional
    @Override
    public void registerAsset(AssetRegisterRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile) {

        // 이미지 파일 디스크 저장 (DB 작업 전에 먼저 수행)
        String storedImageName = fileService.saveImage(imageFile);
        
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
            fileService.deleteFile(storedImageName);
            throw e;
        }
    }

    // 자산 상세조회
    @Override
    public AssetDetailResponseDTO getAssetDetail(Long assetId) {
        PlatformTokenHolding holding = platformTokenHoldingsRepository.findWithTokenAndAssetByAssetId(assetId)
                .orElseThrow(() -> new EntityNotFoundException("자산을 찾을 수 없음"));
        // 자산ID로 공시에서 건물정보에 관한 공시ID조화
        Long disclosureId = disclosureService.getDisclosureBuilding(assetId);
        // PDF원본 파일명
        String pdfName = fileService.getPdfName(disclosureId);
        return adminMapper.toAssetDetailResponseDTO(holding, pdfName);
    }

    // 자산 리스트 조회
    @Override
    public List<AssetListResponseDTO> getAssetList() {
        return tokenRepository.findAllTokensWithAsset()
                .stream()
                .map(token -> adminMapper.toAssetListResponseDTO(token))
                .collect(Collectors.toList());
    }

    // 자산 수정
    @Transactional
    @Override
    public void updateAsset(Long assetId, AssetUpdateRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile) {

        // 기존 자산내역 조회
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new EntityNotFoundException("자산을 찾을 수 없음"));

        // 이미지가 null이 아닐 때만 저장 / 삭제
        String storedImageName = asset.getImgUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 파일 삭제
            fileService.deleteFile(storedImageName);
            // 새 파일 저장
            storedImageName = fileService.saveImage(imageFile);
        }
        // 자산수정
        asset.updateAsset(dto.getAssetName(), dto.getAssetAddress(), storedImageName);

        // 토큰 수정
        Token token = tokenRepository.findById(dto.getTokenId())
                .orElseThrow(() -> new EntityNotFoundException("토큰을 찾을 수 없음"));
        // 토큰 수정
        token.update(dto.getTokenStatus(), dto.getTokenSymbol());

        // pdf파일이 수정됐다면
        if (pdfFile != null && !pdfFile.isEmpty()) {
            // pdf 저장 메소드 호출 (여기에 기존파일 삭제로직 들어감)
            fileService.savePdf(pdfFile, dto.getDisclosureId());
        }
    }

    // 배당 리스트 조회
    @Override
    public List<AllocationListResponseDTO> getAllocationList() {
        // 자산 리스트 조회
        List<Token> tokens = tokenRepository.findAllTokensWithAsset();

        // 배당 이벤트내역 조회
        // 자산ID를 MAP의 키값으로 설정
        Map<Long, AllocationEvent> allocationEventMap = allocationEventRepository.findAllCurrentMonthList()
                .stream()
                .collect(Collectors.toMap(e -> e.getAssetId(), e -> e));

        // assetId를 기준으로 매핑 후 리턴
        return tokens.stream()
                .map(token -> {
                    AllocationEvent event = allocationEventMap.get(token.getAsset().getAssetId());
                    return adminMapper.toAllocationListResponseDTO(token, event);
                }).collect(Collectors.toList());
    }


}

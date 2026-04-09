package server.main.admin.service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.*;
import server.main.admin.entity.Common;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.admin.mapper.AdminMapper;
import server.main.admin.repository.CommonRepository;
import server.main.admin.repository.PlatformTokenHoldingsRepository;
import server.main.allocation.entity.AllocationEvent;
import server.main.allocation.repository.AllocationEventRepository;
import server.main.asset.entity.Asset;
import server.main.asset.service.AssetService;
import server.main.disclosure.service.DisclosureService;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.file.File;
import server.main.global.file.FileService;
import server.main.notice.service.NoticeService;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Log4j2
public class AdminServiceImpl implements AdminService {
    private final PlatformTokenHoldingsRepository platformTokenHoldingsRepository;
    private final AssetService assetService;
    private final TokenRepository tokenRepository;
    private final AdminMapper adminMapper;
    private final NoticeService noticeService;
    private final DisclosureService disclosureService;
    private final FileService fileService;
    private final AllocationEventRepository allocationEventRepository;
    private final CommonRepository commonsRepository;

    // 자산등록
    // 자산 이미지 등록 -> 자산 등록 ->  토큰 등록 -> 플랫폼 소유 토큰 등록 -> 자산 계좌 생성 및 입금 -> 공시 / 공지 등록 -> 첨부파일 등록
    @Transactional
    @Override
    public void registerAsset(AssetRegisterRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile) {

        // 이미지 파일 디스크 저장 (DB 작업 전에 먼저 수행)
        String storedImageName = fileService.saveImage(imageFile);

        try {
            // 자산 정보 먼저 등록
            Asset saveAsset = assetService.registerAsset(adminMapper.toAsset(dto, storedImageName));
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

            // 자산 계좌 생성
            assetService.registerAssetAccount(saveToken);

            // 공지 등록 메서드 호출
            noticeService.registerAssetNotice(dto);

            // 공시 등록 메서드 호출
            String assetName = saveAsset.getAssetName();
            Long assetId = saveAsset.getAssetId();
            Long disclosureId = disclosureService.registerAssetDisclosure(assetName, assetId);

            // PDF파일 저장 (pdf 파일, 공시ID 넣어서 호출)
            fileService.saveOrUpdatePdf(pdfFile, disclosureId);

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
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        // 자산ID로 공시에서 건물정보에 관한 공시ID조화
        Long disclosureId = disclosureService.getDisclosureBuilding(assetId);
        // PDF파일 조회
        File file = fileService.getAssetFile(disclosureId);
        return adminMapper.toAssetDetailResponseDTO(holding, file, disclosureId);
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
        Asset asset = assetService.findById(assetId);

        // 이미지가 null이 아닐 때만 저장 / 삭제
        String storedImageName = asset.getImgUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            // 기존 파일 삭제
            fileService.deleteFile(storedImageName);
            // 새 파일 저장
            storedImageName = fileService.saveImage(imageFile);
        }
        // 자산수정
        asset.updateAsset(dto.getAssetName(), dto.getAssetAddress(), storedImageName, dto.getIsAllocated());

        // 토큰 수정
        Token token = tokenRepository.findById(dto.getTokenId())
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        // 토큰 수정
        token.update(dto.getTokenStatus(), dto.getTokenSymbol());

        // pdf파일이 수정됐다면
        if (pdfFile != null && !pdfFile.isEmpty()) {
            // pdf 저장 메소드 호출 (여기에 기존파일 삭제로직 들어감)
            fileService.saveOrUpdatePdf(pdfFile, dto.getDisclosureId());
        }
    }

    // 배당 리스트 조회
    @Override
    public List<AllocationListResponseDTO> getAllocationList() {
        // 자산 리스트 조회
        List<Token> tokens = tokenRepository.findAllTokensWithAssetAllocationList();

        // 마감월 확인
        YearMonth targetMonth = getTargetMonth();
        // 관리자 마감일
        LocalDate adminTargetMonth = getAdminTargetMonth();
        log.info("관리자 마감일 : {}", adminTargetMonth );
        // 배당 이벤트내역 조회
        // 자산ID를 MAP의 키값으로 설정
        Map<Long, AllocationEvent> allocationEventMap = allocationEventRepository
                .findAllBySettlementMonth(targetMonth.getYear(), targetMonth.getMonthValue())
                .stream()
                .collect(Collectors.toMap(e -> e.getAssetId(), e -> e));
        log.info("배당 이벤트 내역 조회 : {}", allocationEventMap);
        // assetId를 기준으로 매핑 후 리턴
        return tokens.stream()
                .map(token -> {
                    AllocationEvent event = allocationEventMap.get(token.getAsset().getAssetId());
                    return adminMapper.toAllocationListResponseDTO(token, event, targetMonth, adminTargetMonth);
                }).collect(Collectors.toList());
    }

    // 배당 등록
    // 배당 등록 -> 공시 등록 -> 파일 저장
    @Transactional
    @Override
    public void registerAllocation(AllocationRegisterRequestDTO dto, MultipartFile file) {
        // 마감월 확인
        YearMonth targetMonth = getTargetMonth();
        // 년 / 월 추출 (공시글에 추가할거)
        int year = targetMonth.getYear();
        int month = targetMonth.getMonthValue();

        // 해당 월에 이미 등록했는지 검증
        if (allocationEventRepository.existsByAssetIdAndSettlementYearAndSettlementMonth(dto.getAssetId(), targetMonth.getYear(), targetMonth.getMonthValue())) {
            throw new BusinessException(ErrorCode.ALLOCATION_ALREADY_EXISTS);
        }

        // 자산이름 조회
        String assetName = assetService.findAssetName(dto.getAssetId());
        // 공시 자동등록
        Long disclosureId = disclosureService.registerAllocationDisclosure(year, month, assetName, dto.getAssetId());

        // 중복데이터 없다면 등록 진행
        AllocationEvent allocationEvent = AllocationEvent.builder()
                .monthlyDividendIncome(dto.getMonthlyDividendIncome())
                .assetId(dto.getAssetId())
                .allocationBatchStatus(false)
                .settlementYear(targetMonth.getYear())
                .settlementMonth(targetMonth.getMonthValue())
                .disclosureId(disclosureId)
                .build();
        AllocationEvent saveAllocationEvent = allocationEventRepository.save(allocationEvent);
        log.info("배당 이벤트 저장 : {}", saveAllocationEvent);

        // 배당 월수익 입금처리
        assetService.depositAllocationAmount(saveAllocationEvent.getMonthlyDividendIncome(), saveAllocationEvent.getAssetId());

        // 파일저장
        fileService.saveOrUpdatePdf(file, disclosureId);
    }

    // 배당 스케줄내역 상세조회 리스트
    @Override
    public List<AllocationDetailResponseDTO> getAllocationDetailList(Long assetId) {

        // 배당 스케줄 내역 조회
        List<AllocationEvent> events = allocationEventRepository.findAllocationEventsByAssetIdOrderByCreatedAt(assetId);
        // 공시ID 목록 추출
        List<Long> disclosureIds = events.stream()
                .map(allocationEvent -> allocationEvent.getDisclosureId())
                .collect(Collectors.toList());

        // 파일 리스트 추출후 공시ID를 키값으로 MAP 변환
        Map<Long, File> fileMap = fileService.getAllocationFile(disclosureIds)
                .stream()
                .collect(Collectors.toMap(
                   file -> file.getDisclosureId(),
                   file -> file
                ));

        // 파일 서비스에서 공시ID 기준으로 조회 후 MAP으로 조합
        return events.stream()
                .map(event -> {
                    File file = fileMap.get(event.getDisclosureId());
                    return adminMapper.toAllocationDetailResponseDTO(event, file);
                })
                .collect(Collectors.toList());
    }

    // 배당 배치 스케줄 수정
    @Transactional
    @Override
    public void updateAllocation(Long allocationEventId, AllocationUpdateRequestDTO dto, MultipartFile file) {
        // 기존 내역 확인
        AllocationEvent allocationEvent = allocationEventRepository.findById(allocationEventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        // 이미 지급된 배당인지 검증
        if (allocationEvent.getAllocationBatchStatus()) {
            throw new BusinessException(ErrorCode.ALLOCATION_UPDATE_NOT_ALLOWED);
        }
        // 월 수익 수정
        allocationEvent.updateAllocationEvent(dto.getMonthlyDividendIncome());
        // file도 수정됐다면
        if (file != null && !file.isEmpty()) {
            fileService.saveOrUpdatePdf(file, dto.getDisclosureId());
        }
    }

    // 플랫폼 기본설정 (최초 등록 및 수정)
    @Transactional
    @Override
    public void registerCommon(CommonDTO dto) {
        Common common = commonsRepository.findCommon();

        // 최초 등록 시 save
        if (common == null) {
            Common saveCommon = Common.builder()
                    .taxRate(dto.getTaxRate())
                    .chargeRate(dto.getChargeRate())
                    .allocateDate(dto.getAllocateDate())
                    .allocateSetDate(dto.getAllocateSetDate())
                    .build();

            commonsRepository.save(saveCommon);
        } else {
            // 이미 등록되어있다면 update
            common.update(dto.getTaxRate(), dto.getChargeRate(), dto.getAllocateDate(), dto.getAllocateSetDate());
        }
    }

    // 플랫폼 기초설정 조회
    @Override
    public CommonDTO getCommon() {
        Common common = commonsRepository.findCommon();
        return CommonDTO.builder()
                .taxRate(common.getTaxRate())
                .chargeRate(common.getChargeRate())
                .allocateDate(common.getAllocateDate())
                .allocateSetDate(common.getAllocateSetDate())
                .build();
    }

    // 마감월 리턴 메서드
    // 플랫폼설정 테이블에서 마감일을 불러와 마감일보다 지났다면 다음월로 검증됨
    private YearMonth getTargetMonth() {
        Common commons = commonsRepository.findCommon();
        return LocalDate.now().getDayOfMonth() > commons.getAllocateDate()
                ? YearMonth.now().plusMonths(1)
                : YearMonth.now();
    }

    // 관리자 마감일 리턴
    private LocalDate getAdminTargetMonth() {
        Common commons = commonsRepository.findCommon();
        YearMonth targetMonth = LocalDate.now().getDayOfMonth() > commons.getAllocateSetDate()
                ? YearMonth.now().plusMonths(1)
                : YearMonth.now();
        return targetMonth.atDay(commons.getAllocateSetDate());
    }
}

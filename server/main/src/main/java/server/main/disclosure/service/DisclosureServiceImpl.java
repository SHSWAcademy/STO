package server.main.disclosure.service;

import jakarta.persistence.EntityNotFoundException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import server.main.asset.entity.Asset;
import server.main.asset.repository.AssetRepository;
import server.main.disclosure.dto.DisclosureListResponseDTO;
import server.main.disclosure.dto.DisclosureRegisterDTO;
import server.main.disclosure.dto.DisclosureUpdateDTO;
import server.main.disclosure.entity.Disclosure;
import server.main.disclosure.mapper.DisclosureMapper;
import server.main.disclosure.repository.DisclosureRepository;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.file.File;
import server.main.global.file.FileRepository;
import server.main.global.file.FileService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class DisclosureServiceImpl implements DisclosureService{

    private final DisclosureRepository disclosureRepository;
    private final DisclosureMapper disclosureMapper;
    private final AssetRepository assetRepository;
    private final FileService fileService;

    // 공시 자동 등록 (BUILDING)
    @Override
    public Long registerAssetDisclosure(String assetName, Long assetId) {
        Disclosure disclosure = disclosureRepository.save(disclosureMapper.toDisclosure(assetName, assetId));
        log.info("공시 등록내역 확인(BUILDING) : {}", disclosure);
        return disclosure.getDisclosureId();
    }

    // 공시 자동 등록 (DIVIDEND)
    @Override
    public Long registerAllocationDisclosure(int year, int month, String assetName, Long assetId) {
        Disclosure disclosure = disclosureRepository.save(disclosureMapper.toDisclosureAllocation(year, month, assetName, assetId));
        log.info("공시 등록내역 확인(DIVIDEND) : {}", disclosure);
        return disclosure.getDisclosureId();
    }

    // 자산 건물정보 공시 조회
    @Override
    public Long getDisclosureBuilding(Long assetId) {
        Disclosure disclosure = disclosureRepository.findByAssetIdAndCategory(assetId)
                .orElseThrow(() -> new EntityNotFoundException("조회된 공시가 없음"));
        return disclosure.getDisclosureId();
    }

    // 공시 전체 조회
    @Override
    public Page<DisclosureListResponseDTO> getDisclosureList(int page, int size) {
        // 테이블 연관관계 미설정으로 공시, 자산, 파일테이블 조회하여 키값으로 매핑 (N+1 방지를위해 테이블미리 다 조회 후 조합)
        // 공시 테이블 먼저 조회
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Disclosure> disclosures = disclosureRepository.findAll(pageable);

        // 자산id 추출
        List<Long> assetIds = disclosures.stream()
                .map(disclosure -> disclosure.getAssetId())
                .toList();
        // 공시ID 추출
        List<Long> disclosureIds = disclosures.stream()
                .map(disclosure -> disclosure.getDisclosureId())
                .toList();

        // 자산 조회 (자산ID를 키값으로 MAP생성)
        Map<Long, Asset> assetMap = assetRepository.findAllById(assetIds).stream()
                .collect(Collectors.toMap(
                        asset -> asset.getAssetId(),
                        asset -> asset
                        ));

        // 파일조회 (파일ID를 키값으로 MAP생성)
        Map<Long, File> fileMap = fileService.getAllocationFile(disclosureIds).stream()
                .collect(Collectors.toMap(
                        file -> file.getDisclosureId(),
                        file -> file
                ));

        // 자산, 파일 map에서 disclosures의 키값으로 값 추출
        return disclosures.map(disclosure -> {
                    Asset asset = assetMap.get(disclosure.getAssetId());
                    File file = fileMap.get(disclosure.getDisclosureId());
                    // mapper 호출
                    return disclosureMapper.toDisclosureListResponseDTO(disclosure, asset, file);
                });
    }

    // 공시 등록 (admin)
    @Transactional
    @Override
    public void registerDisclosure(DisclosureRegisterDTO dto, MultipartFile file) {
        // 공시 내용먼저 자장
        Disclosure disclosure = disclosureRepository.save(disclosureMapper.toDisclosureRegister(dto));
        log.info("공시 저장내역 확인 : {}", disclosure);
        // 파일 저장
        fileService.saveOrUpdatePdf(file, disclosure.getDisclosureId());
    }

    // 공시 수정 (admin)
    @Transactional
    @Override
    public void updateDisclosure(Long disclosureId, DisclosureUpdateDTO dto, MultipartFile file) {
        // 이전 공시내역 부터 조회
        Disclosure disclosure = disclosureRepository.findById(disclosureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        //  수정 메서드 호출
        disclosure.updateDisclosure(dto.getDisclosureTitle(), dto.getDisclosureContent(), dto.getDisclosureCategory());
        log.info("공시 수정내역 확인 : {}", disclosure);

        // pdf파일이 수정됐다면
        if (file != null && !file.isEmpty()) {
            // pdf 저장 메소드 호출 (여기에 기존파일 삭제로직 들어감)
            fileService.saveOrUpdatePdf(file, disclosureId);
        }
    }

    // 공시 삭제 (admin)
    @Transactional
    @Override
    public void deleteDisclosure(Long disclosureId, String storedName) {

        // 삭제 대상 조회
        Disclosure disclosure = disclosureRepository.findById(disclosureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        // 삭제 처리
        disclosure.softDelete();
        log.info("공시 삭제내역 확인 : {}", disclosure);
        // 첨부파일 삭제처리
        fileService.deleteFile(storedName);
    }

}

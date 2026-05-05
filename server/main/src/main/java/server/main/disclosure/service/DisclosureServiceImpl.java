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
import server.main.global.file.FileService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Log4j2
@RequiredArgsConstructor
public class DisclosureServiceImpl implements DisclosureService {

    private final DisclosureRepository disclosureRepository;
    private final DisclosureMapper disclosureMapper;
    private final AssetRepository assetRepository;
    private final FileService fileService;

    @Override
    public Long registerAssetDisclosure(String assetName, Long assetId) {
        Disclosure disclosure = disclosureRepository.save(disclosureMapper.toDisclosure(assetName, assetId));
        log.info("공시 자동 등록(BUILDING): {}", disclosure);
        return disclosure.getDisclosureId();
    }

    @Override
    public Long registerAllocationDisclosure(int year, int month, String assetName, Long assetId) {
        Disclosure disclosure = disclosureRepository.save(
                disclosureMapper.toDisclosureAllocation(year, month, assetName, assetId)
        );
        log.info("공시 자동 등록(DIVIDEND): {}", disclosure);
        return disclosure.getDisclosureId();
    }

    @Override
    public Long getDisclosureBuilding(Long assetId) {
        Disclosure disclosure = disclosureRepository.findByAssetIdAndCategory(assetId)
                .orElseThrow(() -> new EntityNotFoundException("조회할 공시가 없습니다."));
        return disclosure.getDisclosureId();
    }

    @Override
    public Page<DisclosureListResponseDTO> getDisclosureList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Disclosure> disclosures = disclosureRepository.findAll(pageable);
        return mapDisclosurePage(disclosures);
    }

    @Override
    public Page<DisclosureListResponseDTO> getPublicDisclosureList(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Disclosure> disclosures = disclosureRepository.findByDeletedAtIsNull(pageable);
        return mapDisclosurePage(disclosures);
    }

    private Page<DisclosureListResponseDTO> mapDisclosurePage(Page<Disclosure> disclosures) {
        List<Long> assetIds = disclosures.stream()
                .map(Disclosure::getAssetId)
                .toList();
        List<Long> disclosureIds = disclosures.stream()
                .map(Disclosure::getDisclosureId)
                .toList();

        Map<Long, Asset> assetMap = assetRepository.findAllById(assetIds).stream()
                .collect(Collectors.toMap(Asset::getAssetId, asset -> asset));

        Map<Long, File> fileMap = fileService.getAllocationFile(disclosureIds).stream()
                .collect(Collectors.toMap(File::getDisclosureId, file -> file));

        return disclosures.map(disclosure -> {
            Asset asset = assetMap.get(disclosure.getAssetId());
            File file = fileMap.get(disclosure.getDisclosureId());
            return disclosureMapper.toDisclosureListResponseDTO(disclosure, asset, file);
        });
    }

    @Transactional
    @Override
    public void registerDisclosure(DisclosureRegisterDTO dto, MultipartFile file) {
        Disclosure disclosure = disclosureRepository.save(disclosureMapper.toDisclosureRegister(dto));
        log.info("공시 저장: {}", disclosure);
        fileService.saveOrUpdatePdf(file, disclosure.getDisclosureId());
    }

    @Transactional
    @Override
    public void updateDisclosure(Long disclosureId, DisclosureUpdateDTO dto, MultipartFile file) {
        Disclosure disclosure = disclosureRepository.findById(disclosureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        disclosure.updateDisclosure(dto.getDisclosureTitle(), dto.getDisclosureContent(), dto.getDisclosureCategory());
        log.info("공시 수정: {}", disclosure);

        if (file != null && !file.isEmpty()) {
            fileService.saveOrUpdatePdf(file, disclosureId);
        }
    }

    @Transactional
    @Override
    public void deleteDisclosure(Long disclosureId, String storedName) {
        Disclosure disclosure = disclosureRepository.findById(disclosureId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));
        disclosure.softDelete();
        log.info("공시 삭제: {}", disclosure);
        fileService.deleteFile(storedName);
    }
}

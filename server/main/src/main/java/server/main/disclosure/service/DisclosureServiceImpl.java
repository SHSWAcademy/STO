package server.main.disclosure.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import server.main.disclosure.entity.Disclosure;
import server.main.disclosure.mapper.DisclosureMapper;
import server.main.disclosure.repository.DisclosureRepository;

@Service
@Log4j2
@RequiredArgsConstructor
public class DisclosureServiceImpl implements DisclosureService{

    private final DisclosureRepository disclosureRepository;
    private final DisclosureMapper disclosureMapper;

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
}

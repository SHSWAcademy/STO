package server.main.diclosure.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import server.main.diclosure.dto.DisclosureRegisterAssetDTO;
import server.main.diclosure.entity.Disclosure;
import server.main.diclosure.mapper.DisclosureMapper;
import server.main.diclosure.repository.DisclosureRepository;

@Service
@Log4j2
@RequiredArgsConstructor
public class DisclosureServiceImpl implements DisclosureService{

    private final DisclosureRepository disclosureRepository;
    private final DisclosureMapper disclosureMapper;

    // 공시 자동 등록
    @Override
    public Long registerAssetDisclosure(String assetName, Long assetId) {
        DisclosureRegisterAssetDTO disclosureRegisterAssetDTO = new DisclosureRegisterAssetDTO();
        disclosureRegisterAssetDTO.changeDisclosure(assetName, assetId);
        Disclosure disclosure = disclosureRepository.save(disclosureMapper.toDisclosure(disclosureRegisterAssetDTO));
        log.info("공시 등록내역 확인 : {}", disclosureRegisterAssetDTO);
        return disclosure.getDisclosureId();
    }
}

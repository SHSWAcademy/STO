package server.main.diclosure.mapper;

import org.springframework.stereotype.Component;
import server.main.diclosure.dto.DisclosureRegisterAssetDTO;
import server.main.diclosure.entity.Disclosure;

@Component
public class DisclosureMapper {

    // 자산 등록 시 공시 자동 등록 dto -> entity
    public Disclosure toDisclosure(DisclosureRegisterAssetDTO dto) {
        return Disclosure.builder()
                .assetId(dto.getAssetId())
                .disclosureCategory(dto.getDisclosureCategory())
                .disclosureTitle(dto.getDisclosureTitle())
                .disclosureContent(dto.getDisclosureContent())
                .build();
    }
}

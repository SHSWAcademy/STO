package server.main.diclosure.service;

import server.main.diclosure.dto.DisclosureRegisterAssetDTO;

public interface DisclosureService {
    Long registerAssetDisclosure(String assetName, Long assetId);   // 공시 자동등록
}

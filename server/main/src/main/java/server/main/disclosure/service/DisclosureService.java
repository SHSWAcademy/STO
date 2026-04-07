package server.main.diclosure.service;

public interface DisclosureService {
    Long registerAssetDisclosure(String assetName, Long assetId);   // 공시 자동등록(BUILDING)
    Long registerAllocationDisclosure(int year, int month, String assetName, Long assetId); // 공시 자동등록(DIVIDEND)
    Long getDisclosureBuilding(Long assetId);       // 자산 건물정보 공시 조회
}

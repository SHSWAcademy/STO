package server.main.disclosure.service;

import org.springframework.web.multipart.MultipartFile;
import server.main.disclosure.dto.DisclosureListResponseDTO;
import server.main.disclosure.dto.DisclosureRegisterDTO;
import server.main.disclosure.dto.DisclosureUpdateDTO;

import java.util.List;

public interface DisclosureService {
    Long registerAssetDisclosure(String assetName, Long assetId);   // 공시 자동등록(BUILDING)
    Long registerAllocationDisclosure(int year, int month, String assetName, Long assetId); // 공시 자동등록(DIVIDEND)
    Long getDisclosureBuilding(Long assetId);       // 자산 건물정보 공시 조회
    List<DisclosureListResponseDTO> getDisclosureList(); // 공시 전체 조회
    void registerDisclosure(DisclosureRegisterDTO dto, MultipartFile file);  // 공시 등록 (admin)
    void updateDisclosure(Long disclosureId, DisclosureUpdateDTO dto, MultipartFile file);     // 공시 수정 (admin)
    void deleteDisclosure(Long disclosureId,  String storedName);        // 파일 저장명);   // 공시 삭제 (admin)
}

package server.main.disclosure.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import server.main.disclosure.dto.DisclosureListResponseDTO;
import server.main.disclosure.dto.DisclosureRegisterDTO;
import server.main.disclosure.dto.DisclosureUpdateDTO;

public interface DisclosureService {
    Long registerAssetDisclosure(String assetName, Long assetId);
    Long registerAllocationDisclosure(int year, int month, String assetName, Long assetId);
    Long getDisclosureBuilding(Long assetId);
    Page<DisclosureListResponseDTO> getDisclosureList(int page, int size);
    Page<DisclosureListResponseDTO> getPublicDisclosureList(int page, int size);
    void registerDisclosure(DisclosureRegisterDTO dto, MultipartFile file);
    void updateDisclosure(Long disclosureId, DisclosureUpdateDTO dto, MultipartFile file);
    void deleteDisclosure(Long disclosureId, String storedName);
}

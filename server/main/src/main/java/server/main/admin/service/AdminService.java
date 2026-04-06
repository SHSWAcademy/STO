package server.main.admin.service;

import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.AssetDetailResponseDTO;
import server.main.admin.dto.AssetListResponseDTO;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.admin.dto.AssetUpdateRequestDTO;

import java.util.List;

public interface AdminService {
    void registerAsset(AssetRegisterRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile);       // 자산등록
    AssetDetailResponseDTO getAssetDetail(Long assetId);   // 자산 상세조회
    List<AssetListResponseDTO> getAssetList();             // 자산 리스트 조회
    void updateAsset(Long assetId, AssetUpdateRequestDTO dto, MultipartFile imageFile, MultipartFile pdfFile);   // 자산 수정
}

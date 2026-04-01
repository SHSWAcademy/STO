package server.main.admin.service;

import server.main.admin.dto.AssetRegisterRequestDTO;

public interface AdminService {
    void registerAsset(AssetRegisterRequestDTO dto);    // 자산등록
}

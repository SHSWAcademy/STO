package server.main.notice.service;

import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.asset.entity.Asset;

public interface NoticeService {
    void registerAssetNotice(AssetRegisterRequestDTO dto);
}

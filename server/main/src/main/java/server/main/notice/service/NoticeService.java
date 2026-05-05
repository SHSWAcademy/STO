package server.main.notice.service;

import org.springframework.data.domain.Page;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.notice.dto.NoticeDetailResponseDTO;
import server.main.notice.dto.NoticeListResponseDTO;
import server.main.notice.dto.NoticeRegisterDTO;

public interface NoticeService {
    void registerAssetNotice(AssetRegisterRequestDTO dto);
    Page<NoticeListResponseDTO> getNoticeList(int page, int size);
    Page<NoticeListResponseDTO> getPublicNoticeList(int page, int size);
    void registerNotice(NoticeRegisterDTO dto);
    NoticeDetailResponseDTO getNoticeDetail(Long noticeId);
    NoticeDetailResponseDTO getPublicNoticeDetail(Long noticeId);
    void updateNotice(Long noticeId, NoticeRegisterDTO dto);
    void deleteNotice(Long noticeId);
}

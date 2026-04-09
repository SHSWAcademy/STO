package server.main.notice.service;

import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.asset.entity.Asset;
import server.main.notice.dto.NoticeDetailResponseDTO;
import server.main.notice.dto.NoticeListResponseDTO;
import server.main.notice.dto.NoticeRegisterDTO;

import java.util.List;

public interface NoticeService {
    void registerAssetNotice(AssetRegisterRequestDTO dto); // 자산 등록 시 자동 공지 등록
    List<NoticeListResponseDTO> getNoticeList();    // 공지사항 조회
    void registerNotice(NoticeRegisterDTO dto);     // 공지사항 등록
    NoticeDetailResponseDTO getNoticeDetail(Long noticeId);  // 공지사항 상세조회
    void updateNotice(Long noticeId, NoticeRegisterDTO dto);   // 공지사항 수정 및 삭제
    void deleteNotice(Long noticeId);   // 공지사항 삭제
}

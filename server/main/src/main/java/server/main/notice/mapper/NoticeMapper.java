package server.main.notice.mapper;

import org.springframework.stereotype.Component;
import server.main.notice.dto.NoticeDetailResponseDTO;
import server.main.notice.dto.NoticeListResponseDTO;
import server.main.notice.dto.NoticeRegisterAssetDTO;
import server.main.notice.entity.Notice;

@Component
public class NoticeMapper {

    // 자산 등록 시 공지 자동등록 dto -> entity 변환
    public Notice toNotice (NoticeRegisterAssetDTO dto) {
        return Notice.builder()
                .noticeTitle(dto.getNoticeTitle())
                .noticeContent(dto.getNoticeContent())
                .noticeType(dto.getNoticeType())
                .build();
    }

    // 공지사항 리스트 조회 entity -> dto
    public NoticeListResponseDTO toNoticeAdmin(Notice notice) {
        return NoticeListResponseDTO.builder()
                .noticeId(notice.getNoticeId())
                .noticeTitle(notice.getNoticeTitle())
                .noticeType(notice.getNoticeType())
                .noticeContent(notice.getNoticeContent())
                .deletedAt(notice.getDeletedAt())
                .createdAt(notice.getCreatedAt())
                .build();
    }

    // 공지사항 상세조회
    public NoticeDetailResponseDTO noticeDetailResponseDTO(Notice notice) {
        return NoticeDetailResponseDTO.builder()
                .noticeType(notice.getNoticeType())
                .noticeTitle(notice.getNoticeTitle())
                .noticeContent(notice.getNoticeContent())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}

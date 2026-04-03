package server.main.notice.mapper;

import org.springframework.stereotype.Component;
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
}

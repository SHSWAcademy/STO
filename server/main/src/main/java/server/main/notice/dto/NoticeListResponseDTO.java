    package server.main.notice.dto;

    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Builder;
    import lombok.Data;
    import lombok.NoArgsConstructor;
    import server.main.notice.entity.NoticeType;

    import java.time.LocalDateTime;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Builder
    public class NoticeListResponseDTO {
        private Long noticeId;           // 공지ID
        private NoticeType noticeType;   // 공지 타입
        private String noticeTitle;      // 공지 제목
        private String noticeContent;    // 공지 본문
        private LocalDateTime createdAt; // 작성일자
        private LocalDateTime deletedAt; // 삭제일자
    }

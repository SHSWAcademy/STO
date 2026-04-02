package server.main.notice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.domain.PageRequest;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Table(name = "notices")
public class Notice extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long noticeId;           // 공지ID
    @Enumerated(EnumType.STRING)
    private NoticeType noticeType;   // 공지 타입
    private String noticeTitle;      // 공지 제목

    @Column(name = "notice_content", columnDefinition = "TEXT")
    private String noticeContent;    // 공지 본문
}

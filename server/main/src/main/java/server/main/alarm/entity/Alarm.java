package server.main.alarm.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Builder
@Entity
@Getter
@Table(name = "ALARMS")
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Alarm {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long alarmId;

    @Column(name = "member_id", nullable = false) // 외래키
    private Long memberId;

    @Column(name = "token_id") // 외래키
    private Long tokenId;

    private String alarmContent;

    @Builder.Default
    private Boolean isRead = false;


    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private AlarmType alarmType;

    public void markAsRead() {
        this.isRead = true;
    }
}

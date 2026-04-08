package server.main.log;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@SuperBuilder
@MappedSuperclass
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Getter
public class LogBaseEntity {
    private LocalDateTime timeStamp;
    private String identifier;
    private String task;
    private String detail;
    private Boolean result;
    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;
}

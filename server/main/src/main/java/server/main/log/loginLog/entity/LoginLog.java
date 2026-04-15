package server.main.log.loginLog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.log.LogBaseEntity;

@Entity
@Getter
@Table(name = "LOGIN_LOGS")
@NoArgsConstructor
@SuperBuilder
public class LoginLog extends LogBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "login_log_id")
    private Long loginLogId;
    private String ip;
}

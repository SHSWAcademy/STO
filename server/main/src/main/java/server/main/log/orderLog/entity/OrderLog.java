package server.main.log.orderLog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.log.LogBaseEntity;

@Entity
@Getter
@Table(name = "order_logs")
@NoArgsConstructor
@SuperBuilder
public class OrderLog extends LogBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_log_id")
    private Long orderLogId;
    private String orderType;
}

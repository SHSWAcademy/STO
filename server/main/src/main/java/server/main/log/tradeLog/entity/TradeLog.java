package server.main.log.tradeLog.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.log.LogBaseEntity;

@Entity
@Getter
@Table(name = "TRADE_LOGS")
@NoArgsConstructor
@SuperBuilder
public class TradeLog extends LogBaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_log_id")
    private Long tradeLogId;
}

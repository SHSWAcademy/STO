package server.main.candle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Table(name = "CANDLE_HOURS")
@SuperBuilder
@NoArgsConstructor
public class CandleHour extends Candle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candle_id")
    private Long candleId;
}

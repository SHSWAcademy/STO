package server.main.candle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Entity
@Getter
@Table(name = "CANDLE_MONTHS")
@NoArgsConstructor
@SuperBuilder
public class CandleMonth extends Candle {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "candle_id")
    private Long candleId;
}

package server.main.candle.entity;

import jakarta.persistence.*;
import server.main.token.entity.Token;

import java.time.LocalDateTime;

// Candle 상속 전략 코드, 여유될 때 상속으로 바꿀게요

// @Entity
// @Inheritance(strategy = InheritanceType.SINGLE_TABLE)
// @DiscriminatorColumn
public class Candle {
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    private Double volume;
    private LocalDateTime candleTime;
    private Integer tradeCount;

//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "token_id")
//    private Token token;
}

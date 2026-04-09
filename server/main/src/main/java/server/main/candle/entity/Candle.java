package server.main.candle.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.token.entity.Token;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
@NoArgsConstructor
@SuperBuilder
public abstract class Candle {
    private Double openPrice;
    private Double highPrice;
    private Double lowPrice;
    private Double closePrice;
    private Double volume;              // 해당 구간 체결량 합계
    private LocalDateTime candleTime;   // 1분 단위 타임 스탬프
    private Integer tradeCount;         // 캔들 시간 구간 동안 실제로 거래가 체결된 횟수

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id")
    private Token token;
}

package server.batch.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "TRADES")
@EntityListeners(AuditingEntityListener.class)
public class Trade {

    @Id
    @Column(name = "trade_id")
    private Long tradeId;

    private Long tradePrice;                        // 실제 체결 가격 (1 토큰당 가격)

    private Long tradeQuantity;                     // 실제 체결 수량

    private LocalDateTime executedAt;               // 실제 매칭 엔진 체결 시간

    private Long tokenId;                           // 연관관계 없이 Long 으로 FK를 가진다
}

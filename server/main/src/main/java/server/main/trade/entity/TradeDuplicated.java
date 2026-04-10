package server.main.trade.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "trades_duplicated")
public class TradeDuplicated {

    @Id
    @Column(name = "trade_id")
    private Long tradeId;

    private Long sellerId;
    private Long buyerId;
    private Long sellOrderId;
    private Long buyOrderId;
    private Long tokenId;
    private Long tradePrice;
    private Long tradeQuantity;

    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;

    private LocalDateTime executedAt;
    private Long feeAmount;
    private Long totalTradePrice;
    private LocalDateTime createdAt;
}

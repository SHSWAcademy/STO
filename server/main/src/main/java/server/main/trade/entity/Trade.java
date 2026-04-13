package server.main.trade.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import server.main.member.entity.Member;
import server.main.order.entity.Order;
import server.main.token.entity.Token;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "trade_id")
    private Long tradeId;

    private Long tradePrice;                        // 실제 체결 가격 (1 토큰당 가격)

    private Long tradeQuantity;                     // 실제 체결 수량

    private Long totalTradePrice;                   // 총 체결 금액

    private Long feeAmount;                         // 거래 수수료 (계산된 수수료)

    @Enumerated(EnumType.STRING)
    private SettlementStatus settlementStatus;      // 정산 상태

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;                // 레코드 생성 시간

    @Column(updatable = false)
    private LocalDateTime executedAt;               // 실제 매칭 엔진 체결 시간

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private Member seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id")
    private Member buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buy_order_id")
    private Order buyOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sell_order_id")
    private Order sellOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id")
    private Token token;

    public void updateSettlementStatus(SettlementStatus status) {
        this.settlementStatus = status;
    }
}

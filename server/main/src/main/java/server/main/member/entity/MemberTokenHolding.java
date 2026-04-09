package server.main.member.entity;

import java.time.LocalDateTime;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.token.entity.Token;

@Entity
@Getter
@Table(name = "TOKEN_HOLDINGS")
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class MemberTokenHolding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "token_holding_id")
    private Long tokenHoldingId;

    private LocalDateTime updatedAt; // DB 에서 NOW()로 들어간다

    private Long currentQuantity;     // 현재 회원이 가지고 있는 토큰 보유량
    private Long lockedQuantity;      // 매도 주문으로 묶인 수량
    private Double avgBuyPrice;       // 평균 매수가 (수익률, 평가 손익 계산)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id")
    private Token token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // 매수 체결로 토큰을 처음 받는 경우 — 새 보유 레코드 생성
    public static MemberTokenHolding createForBuyer(Member member, Token token, Long quantity, Long tradePrice) {
        MemberTokenHolding holding = new MemberTokenHolding();
        holding.member = member;
        holding.token = token;
        holding.currentQuantity = quantity;
        holding.lockedQuantity = 0L;
        holding.avgBuyPrice = tradePrice.doubleValue();
        return holding;
    }

    // 매도 호가 시 보유 토큰 감소
    public void lockQuantity(Long amount) {
        this.currentQuantity -= amount;
        this.lockedQuantity += amount;
    }

    // 매도 호가 수정 : 기존 주문 수량을 변경
    public void relockQuantity(Long oldQuantity, Long updateQuantity) {
        this.currentQuantity += oldQuantity;
        this.lockedQuantity -= oldQuantity;

        this.currentQuantity -= updateQuantity;
        this.lockedQuantity += updateQuantity;
    }

    // 매도 호가 취소 : 토큰 수를 다시 되돌려둔다
    public void cancelOrder(Long orderQuantity) {
        this.currentQuantity += orderQuantity;
        this.lockedQuantity -= orderQuantity;
    }
    
    // 매도 체결 시 묶인 수량 차감
    public void settleSellTrade(Long quantity) {
        this.lockedQuantity -= quantity;
    }

    // 매수 체결 시 토큰 수령 + 평균 매수가 갱신
    public void settleBuyTrade(Long quantity, Long tradePrice) {
        double newAvg = (
            (this.currentQuantity * this.avgBuyPrice) + (quantity * tradePrice)) 
            / (this.currentQuantity + quantity);
        this.currentQuantity += quantity;
        this.avgBuyPrice = newAvg;
    }
}

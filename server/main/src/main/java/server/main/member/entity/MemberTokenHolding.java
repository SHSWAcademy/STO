package server.main.member.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import server.main.token.entity.Token;

import java.time.LocalDateTime;

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

    // 매도 호가 시 보유 토큰 감소
    public void lockQuantity(Long amount) {
        this.currentQuantity -= amount;
        this.lockedQuantity += amount;
    }

    public void relockQuantity(Long oldQuantity, Long updateQuantity) {
        this.currentQuantity += oldQuantity;
        this.lockedQuantity -= oldQuantity;

        this.currentQuantity -= updateQuantity;
        this.lockedQuantity += updateQuantity;
    }
}

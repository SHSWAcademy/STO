package server.main.admin.entity;

import jakarta.persistence.*;
import lombok.*;
import server.main.token.entity.Token;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
@Table(name = "platform_token_holdings")
public class PlatformTokenHolding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long platFormTokenHoldingId;    // 지분ID

    @OneToOne
    @JoinColumn(name = "token_id")
    private Token token;                   // 토큰

    private Long holdingSupply;            // 플랫폼 보유 토큰 수량
    private Long initPrice;                // 토큰 초기 가격
    private LocalDateTime createdAt;       // 생성일자

    // baseEntity 상속 안받을때 PrePersist쓰면 now()자동으로 된대요
    // DB 저장 직전에 자동 실행
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
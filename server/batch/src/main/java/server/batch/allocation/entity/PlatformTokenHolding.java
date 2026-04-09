package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "platform_token_holdings")
public class PlatformTokenHolding {

    @Id
    private Long platformTokenHoldingId;
    private Long tokenId;                   // 토큰ID
    private Long holdingSupply;            // 플랫폼 보유 토큰 수량
    private Long initPrice;                // 토큰 초기 가격
    private LocalDateTime createdAt;       // 생성일자
}

package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "token_holdings")
public class TokenHolding {

    @Id
    private Long tokenHoldingId;

    private Long memberId;
    private Long tokenId;
    private Long walletId;
    private Long currentQuantity;
    private Long lockedQuantity;
}

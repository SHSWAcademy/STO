package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "platform_banking")
public class PlatformBanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long platformBankingId;

    private Long tokenId;
    private Long tradeId;
    private String accountType;
    private Long platformBankingAmount;
    private String platformBankingDirection;
    private LocalDateTime createdAt;

    @Builder
    public PlatformBanking(Long tokenId, String accountType,
                           Long platformBankingAmount, String platformBankingDirection) {
        this.tokenId = tokenId;
        this.accountType = accountType;
        this.platformBankingAmount = platformBankingAmount;
        this.platformBankingDirection = platformBankingDirection;
        this.createdAt = LocalDateTime.now();
    }
}

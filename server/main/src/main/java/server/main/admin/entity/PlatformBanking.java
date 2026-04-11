package server.main.admin.entity;

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

    private Long tokenId;   // 토큰ID
    private Long platformBankingAmount; // 입금액
    private LocalDateTime createdAt;

    @Enumerated(value = EnumType.STRING)
    private PlatformAccountType accountType;

    @Enumerated(value = EnumType.STRING)
    private PlatformDirection platformBankingDirection;

    @Builder
    public PlatformBanking(Long tokenId, PlatformAccountType accountType,
                           Long platformBankingAmount, PlatformDirection platformBankingDirection) {
        this.tokenId = tokenId;
        this.accountType = accountType;
        this.platformBankingAmount = platformBankingAmount;
        this.platformBankingDirection = platformBankingDirection;
        this.createdAt = LocalDateTime.now();
    }
}

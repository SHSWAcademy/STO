package server.main.admin.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "platform_banking")
public class PlatformBanking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long platformBankingId;

    private Long tokenId;   // 토큰ID
    // 추후 자산별 수수료 내역 조회를 위해 추가 (현재 미사용)
    private Long tradeId;
    private Long platformBankingAmount; // 입금액
    private LocalDateTime createdAt;

    @Enumerated(value = EnumType.STRING)
    private PlatformAccountType accountType;

    @Enumerated(value = EnumType.STRING)
    private PlatformDirection platformBankingDirection;

    @Builder
    public PlatformBanking(Long tokenId, Long tradeId, PlatformAccountType accountType,
                           Long platformBankingAmount, PlatformDirection platformBankingDirection) {
        this.tokenId = tokenId;
        this.tradeId = tradeId;
        this.accountType = accountType;
        this.platformBankingAmount = platformBankingAmount;
        this.platformBankingDirection = platformBankingDirection;
        this.createdAt = LocalDateTime.now();
    }
}

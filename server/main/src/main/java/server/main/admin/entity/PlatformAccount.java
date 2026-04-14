package server.main.admin.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "platform_accounts")
public class PlatformAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long platformAccountId;

    private Long platformAccountBalance;
    private Long totalEarned;
    private Long totalWithdrawn;
    private LocalDateTime updatedAt;

    // 수수료 적립: 잔액 + 누적수익 증가
    public void earnFee(Long amount) {
        this.platformAccountBalance += amount;
        this.totalEarned += amount;
        this.updatedAt = LocalDateTime.now();
    }
}

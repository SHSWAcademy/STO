package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@NoArgsConstructor
@Table(name = "platform_accounts")
public class PlatformAccount {

    @Id
    private Long platformAccountId;

    private Long platformAccountBalance;    // 출금가능잔고
    private Long totalEarned;               // 수익 누계
    private Long totalWithdrawn;            // 출금 누계
    private LocalDateTime updatedAt;        // 업데이트

    public void deposit(long amount) {
        this.platformAccountBalance += amount;
        this.totalEarned += amount;
    }
}

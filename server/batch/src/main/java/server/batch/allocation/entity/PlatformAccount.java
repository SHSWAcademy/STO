package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "platform_accounts")
public class PlatformAccount {

    @Id
    private Long platformAccountId;

    private Long platformAccountBalance;
    private Long totalEarned;
    private Long totalWithdrawn;

    public void deposit(long amount) {
        this.platformAccountBalance += amount;
        this.totalEarned += amount;
    }
}

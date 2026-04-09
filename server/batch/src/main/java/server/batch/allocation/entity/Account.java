package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "accounts")
public class Account {

    @Id
    private Long accountId;     // 멤버 계좌ID

    private Long memberId;      // 멤버ID
    private String accountNumber;   // 계좌번호
    private String accountPassword; // 계좌비밀번호
    private Long availableBalance;  // 금액
    private Long lockedBalance;     // 주문으로 묶인 현금

    public void deposit(long amount) {
        this.availableBalance += amount;
    }
}

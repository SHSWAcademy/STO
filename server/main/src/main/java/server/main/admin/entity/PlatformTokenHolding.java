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
@Table(name = "PLATFORM_TOKEN_HOLDINGS")
public class PlatformTokenHolding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "platform_token_holding_id")
    private Long platformTokenHoldingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private Admin admin;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "token_id")
    private Token token;

    @Column(name = "holding_supply")
    private Long holdingSupply;

    @Column(name = "init_price")
    private Long initPrice;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}

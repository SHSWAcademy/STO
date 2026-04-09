package server.main.blockchain.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.global.util.BaseEntity;
import server.main.trade.entity.Trade;

@Entity
@Getter
@NoArgsConstructor
@SuperBuilder
@Table(name = "BLOCKCHAIN_OUTBOX_Q")
public class BlockchainOutboxQ extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "queue_id")
    private Long queueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = true)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_token_holding_id", nullable = false)
    private PlatformTokenHolding platformTokenHolding;

    @Column(name = "payload_json", columnDefinition = "TEXT", nullable = false)
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private QueueStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "last_error_message")
    private String lastErrorMessage;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(name = "max_retry", nullable = false)
    private int maxRetry;

    public void markProcessing() {
        this.status = QueueStatus.PROCESSING;
    }

    public void markSubmitted() {
        this.status = QueueStatus.SUBMITTED;
    }

    public void markConfirmed() {
        this.status = QueueStatus.CONFIRMED;
    }

    public void markFailed(String errorMessage) {
        this.status = QueueStatus.FAILED;
        this.lastErrorMessage = errorMessage;
    }

    public void markAbandoned(String errorMessage) {
        this.status = QueueStatus.ABANDONED;
        this.lastErrorMessage = errorMessage;
    }

    public void incrementRetry() {
        this.retryCount++;
    }

    public boolean isMaxRetryExceeded() {
        return this.retryCount >= this.maxRetry;
    }
}

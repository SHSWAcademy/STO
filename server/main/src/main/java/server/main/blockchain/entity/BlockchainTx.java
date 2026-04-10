package server.main.blockchain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GeneratedColumn;
import org.springframework.stereotype.Component;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.trade.entity.Trade;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Table(name = "BLOCKCHAIN_TX")
public class BlockchainTx {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tx_id")
    private Long txId;

    @Column(name = "queue_id")
    private Long queueId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trade_id", nullable = true)
    private Trade trade;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "platform_token_holding_id", nullable = false)
    private PlatformTokenHolding platformTokenHolding;

    @Column(name = "tx_hash", unique = true, length = 255)
    private String txHash;

    @Column(name = "from_address", length = 255)
    private String fromAddress;

    @Column(name ="to_address", length = 255)
    private String toAddress;

    @Column(name = "contract_address", nullable = false, length = 255)
    private String contractAddress;

    @Column(name = "gas_used")
    private Long gasUsed;

    @Column(name = "block_number")
    private Long blockNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_status", nullable = false)
    private BlockchainTxStatus txStatus;

    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false)
    private BlockchainTxType txType;

}

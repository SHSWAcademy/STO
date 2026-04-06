package server.main.allocation.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder(toBuilder = true)
@ToString
@Table(name = "ALLOCATION_PAYOUTS")
public class AllocationPayout {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "allocation_payout_id")
    private Long allocationPayoutId;
    private Long allocationEventId;         // 배당 관리 ID
    private Long tokenId;
    private Long memberIncome;
    private Long holdingQuantity;
    private AllocationPayoutStatus allocationPayoutStatus;

}

package server.batch.allocation.entity;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@NoArgsConstructor
@Table(name = "allocation_payouts")
public class AllocationPayout {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long allocationPayoutId;    // 배당 지급 내역

    private Long memberId;      // 멤버ID
    private Long allocationEventId; // 배당관리ID
    private Long tokenId;           // 토큰ID
    private Long memberIncome;      // 회원 배당 ㄱ므액
    private Long holdingQuantity;   // 배당 기준일 당시 토큰 보유 소유량

    @Enumerated(EnumType.STRING)
    private PayoutStatus status;    // 대기 / 성공 / 실패

    private LocalDateTime createdAt;

    // 배당
    @Builder
    public AllocationPayout(Long memberId, Long allocationEventId, Long tokenId,
                            Long memberIncome, Long holdingQuantity, PayoutStatus status) {
        this.memberId = memberId;
        this.allocationEventId = allocationEventId;
        this.tokenId = tokenId;
        this.memberIncome = memberIncome;
        this.holdingQuantity = holdingQuantity;
        this.status = status;
        this.createdAt = LocalDateTime.now();
    }
}

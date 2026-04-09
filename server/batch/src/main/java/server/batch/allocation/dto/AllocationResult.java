package server.batch.allocation.dto;

import lombok.Builder;
import lombok.Getter;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.entity.AssetAccount;
import server.batch.allocation.entity.PlatformAccount;

import java.util.List;

@Getter
@Builder
public class AllocationResult {

    private AllocationEvent event;

    private AssetAccount assetAccount;
    private long totalDeduction;

    private List<MemberPayoutResult> memberPayouts;

    private PlatformAccount platformAccount;
    private long platformAmount;
    private Long tokenId;
}

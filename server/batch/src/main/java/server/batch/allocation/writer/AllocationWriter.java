package server.batch.allocation.writer;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;
import server.batch.allocation.dto.AllocationResult;

@Component
@RequiredArgsConstructor
public class AllocationWriter implements ItemWriter<AllocationResult> {

    @Override
    public void write(Chunk<? extends AllocationResult> chunk) throws Exception {
        for (AllocationResult result : chunk.getItems()) {
            // TODO: 비즈니스 로직 구현
            // 1. asset_accounts 차감 + asset_bankings 저장
            // 2. accounts 증가 + bankings 저장 (회원별)
            // 3. platform_accounts 증가 + platform_banking 저장
            // 4. allocation_payouts 저장
            // 5. allocation_events 완료 처리
        }
    }
}

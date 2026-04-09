package server.batch.allocation.processor;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import server.batch.allocation.dto.AllocationResult;
import server.batch.allocation.entity.AllocationEvent;

@Component
@RequiredArgsConstructor
public class AllocationProcessor implements ItemProcessor<AllocationEvent, AllocationResult> {

    @Override
    public AllocationResult process(AllocationEvent event) throws Exception {
        // TODO: 비즈니스 로직 구현
        // 1. asset_id → token_id 조회
        // 2. token_holdings 조회 (current_quantity > 0)
        // 3. platform_token_holdings SUM 조회
        // 4. 전체 수량 계산
        // 5. 회원별 배당금 계산 (long 캐스팅)
        // 6. 플랫폼 배당금 계산
        return null;
    }
}

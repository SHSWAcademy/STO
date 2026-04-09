package server.batch.allocation.reader;

import org.springframework.batch.item.support.ListItemReader;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.repository.AllocationEventRepository;

public class AllocationEventReader extends ListItemReader<AllocationEvent> {

    // 배치 스케줄 테이블에서 배당이 직급되지 않은 데이터만 조회
    public AllocationEventReader(AllocationEventRepository allocationEventRepository) {
        super(allocationEventRepository.findByAllocationBatchStatusFalse());
    }
}

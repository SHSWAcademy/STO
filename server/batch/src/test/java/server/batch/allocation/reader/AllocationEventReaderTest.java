package server.batch.allocation.reader;

import lombok.extern.log4j.Log4j2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.repository.AllocationEventRepository;

import java.util.List;

@Log4j2
@SpringBootTest
class AllocationEventReaderTest {

    @Autowired
    private AllocationEventRepository allocationEventRepository;

    // 배치 대상자 조회
    @Test
    void allocation_batch_status() {
        List<AllocationEvent> list = allocationEventRepository.findByAllocationBatchStatusFalse();
        log.info("대상 조회 : {}", list);

    }


}

package server.batch.allocation.processor;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import server.batch.allocation.dto.AllocationResult;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.reader.AllocationEventReader;
import server.batch.allocation.repository.AllocationEventRepository;

@Log4j2
@SpringBootTest
class AllocationProcessorTest {

    @Autowired
    private AllocationProcessor processor;

    @Autowired
    private AllocationEventRepository allocationEventRepository;

    @Test
    void process_allocation_event() throws Exception {
        AllocationEventReader reader = new AllocationEventReader(allocationEventRepository);

        AllocationEvent event;
        while ((event = reader.read()) != null) {
            AllocationResult result = processor.process(event);
            log.info("event : {}", event);
            log.info("result : {}", result);
        }
    }

}

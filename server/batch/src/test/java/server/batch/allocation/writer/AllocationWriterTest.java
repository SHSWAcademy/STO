package server.batch.allocation.writer;

import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.Chunk;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import server.batch.allocation.dto.AllocationResult;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.processor.AllocationProcessor;
import server.batch.allocation.reader.AllocationEventReader;
import server.batch.allocation.repository.AllocationEventRepository;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@SpringBootTest(properties = {
        "blockchain.rpc-url=http://localhost:8545",
        "blockchain.issuer-private-key=0x0000000000000000000000000000000000000000000000000000000000000001",
})
class AllocationWriterTest {

    @Autowired
    private AllocationProcessor processor;

    @Autowired
    private AllocationWriter writer;

    @Autowired
    private AllocationEventRepository allocationEventRepository;

    @Test
    void write_allocation_result() throws Exception {
        AllocationEventReader reader = new AllocationEventReader(allocationEventRepository);

        List<AllocationResult> results = new ArrayList<>();

        AllocationEvent event;
        while ((event = reader.read()) != null) {
            AllocationResult result = processor.process(event);
            if (result != null) {
                results.add(result);
            }
        }

        for (AllocationResult result : results) {
            writer.write(new Chunk<>(List.of(result)));
        }
    }

}

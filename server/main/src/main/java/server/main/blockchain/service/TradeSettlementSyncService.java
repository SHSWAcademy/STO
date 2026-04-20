package server.main.blockchain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.blockchain.entity.BlockchainOutboxQ;
import server.main.blockchain.entity.QueueStatus;
import server.main.blockchain.repository.BlockchainOutboxQRepository;
import server.main.trade.entity.SettlementStatus;
import server.main.trade.entity.Trade;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeSettlementSyncService {
    private final BlockchainOutboxQRepository blockchainOutboxQRepository;

    @Scheduled(fixedDelay = 30000)
    @Transactional
    public void syncSettlementStatus() {
        List<BlockchainOutboxQ> confirmedList =
                blockchainOutboxQRepository.findConfirmedWithPendingTrades(
                        QueueStatus.CONFIRMED,
                        SettlementStatus.ON_CHAIN_PENDING
                );

        if (!confirmedList.isEmpty()) {
            confirmedList.forEach(outboxQ ->
                    outboxQ.getTrade().updateSettlementStatus(SettlementStatus.SUCCESS)
            );
            log.info("trade settlementStatus 업데이트 완료: {}건", confirmedList.size());
        }
    }
}

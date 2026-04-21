package server.main.blockchain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.admin.event.TradeFlowEvent;
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
    private final ApplicationEventPublisher eventPublisher;

    @Scheduled(fixedDelay = 10000)
    @Transactional
    public void syncSettlementStatus() {
        List<BlockchainOutboxQ> confirmedList =
                blockchainOutboxQRepository.findConfirmedWithPendingTrades(
                        QueueStatus.CONFIRMED,
                        SettlementStatus.ON_CHAIN_PENDING
                );

        if (!confirmedList.isEmpty()) {
            for (BlockchainOutboxQ outboxQ : confirmedList) {
                outboxQ.getTrade().updateSettlementStatus(SettlementStatus.SUCCESS);
                Trade trade = outboxQ.getTrade();
                eventPublisher.publishEvent(new TradeFlowEvent(
                        "SUCCESS",
                        trade.getTradeId(),
                        trade.getToken().getTokenId(),
                        trade.getToken().getTokenSymbol(),
                        trade.getTotalTradePrice(),
                        trade.getTradeQuantity(),
                        trade.getBuyer().getMemberName(),
                        trade.getSeller().getMemberName()
                ));
            }
            log.info("trade settlementStatus 업데이트 완료: {}건", confirmedList.size());
            List<BlockchainOutboxQ> abandonedList =
                    blockchainOutboxQRepository.findConfirmedWithPendingTrades(
                            QueueStatus.ABANDONED,
                            SettlementStatus.ON_CHAIN_PENDING
                    );

            if (!abandonedList.isEmpty()) {
                abandonedList.forEach(outboxQ ->
                        outboxQ.getTrade().updateSettlementStatus(SettlementStatus.FAILED)
                );
                log.info("trade settlementStatus FAILED 업데이트 완료: {}건", abandonedList.size());
            }
        }
    }
}

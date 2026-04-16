package server.main.blockchain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import server.main.blockchain.StoToken;
import server.main.blockchain.dto.RecordTradePayload;
import server.main.blockchain.entity.*;
import server.main.blockchain.repository.BlockchainOutboxQRepository;
import server.main.blockchain.repository.BlockchainTxRepository;
import server.main.trade.entity.SettlementStatus;
import server.main.trade.repository.TradeRepository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainWorkerService {

    private final BlockchainOutboxQRepository blockchainOutboxQRepository;
    private final BlockchainTxRepository blockchainTxRepository;
    private final Web3j web3j;
    private final Credentials issuerCredentials;
    private final ObjectMapper objectMapper;

    @Transactional
    public void processPending() {
        List<BlockchainOutboxQ> pendingList = blockchainOutboxQRepository.findByStatusInWithFetch(List.of(QueueStatus.PENDING, QueueStatus.FAILED));

        for (BlockchainOutboxQ blockchainOutboxQ : pendingList) {
            try {
                blockchainOutboxQ.markProcessing();

                RecordTradePayload payload = objectMapper.readValue(
                        blockchainOutboxQ.getPayloadJson(), RecordTradePayload.class
                );

                StoToken stoToken = StoToken.load(
                        payload.getContractAddress(),
                        web3j,
                        issuerCredentials,
                        new DefaultGasProvider()
                );

                blockchainOutboxQ.markSubmitted();

                TransactionReceipt receipt = stoToken.recordTrade(
                        BigInteger.valueOf(blockchainOutboxQ.getTrade().getTradeId()),
                        payload.getSellerAddress(),
                        payload.getBuyerAddress(),
                        BigInteger.valueOf(payload.getQuantity()),
                        BigInteger.valueOf(payload.getPrice())
                ).send();

                boolean isSuccess = "0x1".equals(receipt.getStatus());
                LocalDateTime now = LocalDateTime.now();

                if (isSuccess) {
                    blockchainOutboxQ.markConfirmed();
                    blockchainOutboxQ.getTrade().updateSettlementStatus(SettlementStatus.SUCCESS);

                } else {
                    blockchainOutboxQ.incrementRetry();
                    if (blockchainOutboxQ.isMaxRetryExceeded()) {
                        blockchainOutboxQ.markAbandoned("Transaction reverted on-chain");
                        blockchainOutboxQ.getTrade().updateSettlementStatus(SettlementStatus.FAILED);
                    } else {
                        blockchainOutboxQ.markFailed("Transaction reverted on-chain");
                    }
                }

                BlockchainTx blockchainTx = BlockchainTx.builder()
                        .queueId(blockchainOutboxQ.getQueueId())
                        .trade(blockchainOutboxQ.getTrade())
                        .platformTokenHolding(blockchainOutboxQ.getPlatformTokenHolding())
                        .txHash(receipt.getTransactionHash())
                        .fromAddress(issuerCredentials.getAddress())
                        .toAddress(payload.getContractAddress())
                        .contractAddress(payload.getContractAddress())
                        .gasUsed(receipt.getGasUsed().longValue())
                        .blockNumber(receipt.getBlockNumber().longValue())
                        .txStatus(isSuccess ? BlockchainTxStatus.CONFIRMED : BlockchainTxStatus.REVERTED)
                        .txType(BlockchainTxType.TRADE)
                        .submittedAt(now)
                        .confirmedAt(isSuccess ? now : null)
                        .build();

                blockchainTxRepository.save(blockchainTx);

                log.info("온체인 기록 완료 queueId: {} txHash: {}", blockchainOutboxQ.getQueueId(), receipt.getTransactionHash());
            } catch (Exception e) {
                blockchainOutboxQ.incrementRetry();
                if (blockchainOutboxQ.isMaxRetryExceeded()) {
                    blockchainOutboxQ.markAbandoned(e.getMessage());
                    blockchainOutboxQ.getTrade().updateSettlementStatus(SettlementStatus.FAILED);
                } else {
                    blockchainOutboxQ.markFailed(e.getMessage());
                }
               log.error("온체인 기록 실패 queueId: {} retryCount: {} maxRetry: {}", blockchainOutboxQ.getQueueId(), blockchainOutboxQ.getRetryCount(), blockchainOutboxQ.getMaxRetry(), e);
            }
        }
    }
}

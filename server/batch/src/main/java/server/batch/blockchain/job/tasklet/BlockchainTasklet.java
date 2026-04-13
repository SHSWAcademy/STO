package server.batch.blockchain.job.tasklet;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.gas.DefaultGasProvider;
import server.batch.blockchain.StoToken;
import server.batch.blockchain.dto.RecordTradePayload;
import server.batch.blockchain.entity.*;
import server.batch.blockchain.repository.BlockchainOutboxQRepository;
import server.batch.blockchain.repository.BlockchainTxRepository;

import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class BlockchainTasklet implements Tasklet {

    private final BlockchainOutboxQRepository blockchainOutboxQRepository;
    private final BlockchainTxRepository blockchainTxRepository;
    private final Web3j web3j;
    private final Credentials issuerCredentials;
    private final ObjectMapper objectMapper;


    @Nullable
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) throws Exception {

        List<BlockchainOutboxQ> pendingList = blockchainOutboxQRepository
                .findByStatusIn(List.of(QueueStatus.PENDING, QueueStatus.FAILED, QueueStatus.PROCESSING));

        log.info("블록체인 배치 시작 - 처리 대상: {}건", pendingList.size());

        for (BlockchainOutboxQ blockchainOutboxQ : pendingList) {

            try {
                blockchainOutboxQ.markProcessing();
                blockchainOutboxQRepository.save(blockchainOutboxQ);

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
                blockchainOutboxQRepository.save(blockchainOutboxQ);

                TransactionReceipt receipt = stoToken.recordTrade(
                        BigInteger.valueOf(blockchainOutboxQ.getTradeId()),
                        payload.getSellerAddress(),
                        payload.getBuyerAddress(),
                        BigInteger.valueOf(payload.getQuantity()),
                        BigInteger.valueOf(payload.getPrice())
                ).send();

                boolean isSuccess = "0x1".equals(receipt.getStatus());
                LocalDateTime now = LocalDateTime.now();

                if (isSuccess) {
                    blockchainOutboxQ.markConfirmed();
                } else {
                    blockchainOutboxQ.incrementRetry();
                    if (blockchainOutboxQ.isMaxRetryExceeded()) {
                        blockchainOutboxQ.markAbandoned("Transaction reverted on-chain");
                    } else {
                        blockchainOutboxQ.markFailed("Transaction reverted on-chain");
                    }
                }

                blockchainOutboxQRepository.save(blockchainOutboxQ);

                BlockchainTx blockchainTx = BlockchainTx.builder()
                        .queueId(blockchainOutboxQ.getQueueId())
                        .tradeId(blockchainOutboxQ.getTradeId())
                        .platformTokenHoldingId(blockchainOutboxQ.getPlatformTokenHoldingId())
                        .txHash(receipt.getTransactionHash())
                        .fromAddress(issuerCredentials.getAddress())
                        .toAddress(payload.getContractAddress())
                        .contractAddress(payload.getContractAddress())
                        .gasUsed(receipt.getGasUsed().longValue())
                        .blockNumber(receipt.getBlockNumber().longValue())
                        .txStatus(isSuccess ? BlockchainTxStatus.CONFIRMED : BlockchainTxStatus.REVERTED)
                        .txType(BlockchainTxType.TRADE)
                        .submittedAt(now)
                        .confirmedAt(isSuccess ? now :null)
                        .build();
                blockchainTxRepository.save(blockchainTx);

                log.info("온체인 기록 완료 queueId: {} txHash: {}", blockchainOutboxQ.getQueueId(), receipt.getTransactionHash());
            } catch (Exception e) {
                blockchainOutboxQ.incrementRetry();
                if (blockchainOutboxQ.isMaxRetryExceeded()) {
                    blockchainOutboxQ.markAbandoned(e.getMessage());
                } else {
                    blockchainOutboxQ.markFailed(e.getMessage());
                }
                blockchainOutboxQRepository.save(blockchainOutboxQ);
                log.error("온체인 기록 실패 queueId: {} retryCount: {}", blockchainOutboxQ.getQueueId(), blockchainOutboxQ.getRetryCount(), e);
            }
        }
        return RepeatStatus.FINISHED;
    }
}

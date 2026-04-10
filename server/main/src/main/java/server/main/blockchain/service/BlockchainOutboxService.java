package server.main.blockchain.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.admin.repository.PlatformTokenHoldingsRepository;
import server.main.blockchain.dto.RecordTradePayload;
import server.main.blockchain.entity.BlockchainOutboxQ;
import server.main.blockchain.entity.QueueStatus;
import server.main.blockchain.repository.BlockchainOutboxQRepository;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.member.repository.WalletRepository;
import server.main.token.entity.Token;
import server.main.trade.entity.Trade;

@Slf4j
@Service
@RequiredArgsConstructor
public class BlockchainOutboxService {

    private final BlockchainOutboxQRepository blockchainOutboxQRepository;
    private final PlatformTokenHoldingsRepository platformTokenHoldingsRepository;
    private final WalletRepository walletRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void saveTradeOutbox(Trade trade, Token token) {

        String idempotencyKey = "TRADE-" + trade.getTradeId();
        if (blockchainOutboxQRepository.existsByIdempotencyKey(idempotencyKey)) {
            log.warn("이미 등록된 BlockchainOutboxQ.idempotencyKey: {}", idempotencyKey);
            return;
        }

        String sellerAddress = walletRepository.findByMember(trade.getSeller())
                .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_WALLET_NOT_FOUND))
                .getWalletAddress();

        String buyerAddress = walletRepository.findByMember(trade.getBuyer())
                .orElseThrow(() -> new BusinessException(ErrorCode.BUYER_WALLET_NOT_FOUND))
                .getWalletAddress();

        PlatformTokenHolding platformTokenHolding = platformTokenHoldingsRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUNT_ERROR));

        RecordTradePayload payload = RecordTradePayload.builder()
                .contractAddress(token.getContractAddress())
                .sellerAddress(sellerAddress)
                .buyerAddress(buyerAddress)
                .quantity(trade.getTradeQuantity())
                .price(trade.getTradePrice())
                .build();

        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            BlockchainOutboxQ blockchainOutboxQ = BlockchainOutboxQ.builder()
                    .trade(trade)
                    .platformTokenHolding(platformTokenHolding)
                    .payloadJson(payloadJson)
                    .status(QueueStatus.PENDING)
                    .retryCount(0)
                    .maxRetry(3)
                    .idempotencyKey(idempotencyKey)
                    .build();

            blockchainOutboxQRepository.save(blockchainOutboxQ);
            log.info("BlockchainOutboxQ 저장 완료 tradeId: {}", trade.getTradeId());
        } catch (JsonProcessingException e) {
            log.error("BlockchainOutboxQ payload 직렬화 실패 tradeId: {}", trade.getTradeId(), e);
            throw new BusinessException(ErrorCode.ONCHAIN_TRANSACTION_FAILED);
        }
    }
}

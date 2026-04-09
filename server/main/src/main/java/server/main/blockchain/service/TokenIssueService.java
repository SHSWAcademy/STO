package server.main.blockchain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;
import server.main.blockchain.StoToken;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.member.repository.WalletRepository;
import server.main.trade.entity.Trade;

import java.math.BigInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenIssueService {

    private final Web3j web3j;
    private final Credentials issuerCredentials;
    private final WalletRepository walletRepository;

    public void recordTrade(Trade trade) {

        try {
            String contractAddress = trade.getToken().getContractAddress();

            String sellerAddress = walletRepository.findByMember(trade.getSeller())
                    .orElseThrow(() -> new BusinessException(ErrorCode.SELLER_WALLET_NOT_FOUND))
                    .getWalletAddress();

            String buyerAddress = walletRepository.findByMember(trade.getBuyer())
                    .orElseThrow(() -> new BusinessException(ErrorCode.BUYER_WALLET_NOT_FOUND))
                    .getWalletAddress();

            StoToken stokToken = StoToken.load(
                    contractAddress,
                    web3j,
                    issuerCredentials,
                    new DefaultGasProvider()
            );

            stokToken.recordTrade(
                    BigInteger.valueOf(trade.getTradeId()),
                    sellerAddress,
                    buyerAddress,
                    BigInteger.valueOf(trade.getTradeQuantity()),
                    BigInteger.valueOf(trade.getTradePrice())
            ).send();

            log.info("온체인 거래 기록 완료 tradeId: {}", trade.getTradeId());
        } catch (Exception e) {
            log.error("온체인 거래 기록 실패 tradeId: {}", trade.getTradeId(), e);
            throw new BusinessException(ErrorCode.ONCHAIN_TRANSACTION_FAILED);
        }
    }
}

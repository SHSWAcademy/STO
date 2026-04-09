package server.main.blockchain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.gas.DefaultGasProvider;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.blockchain.StoToken;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.member.entity.WalletRole;
import server.main.member.repository.WalletRepository;
import server.main.token.entity.Token;

import java.math.BigInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractGatewayService {

    private final Web3j web3j;
    private final Credentials issuerCredentials;
    private final WalletRepository walletRepository;

    public String deployToken(Token token, PlatformTokenHolding platformTokenHolding) {

        try {
            String issuerAddress = walletRepository.findByWalletRole(WalletRole.ISSUER)
                    .orElseThrow(() -> new BusinessException(ErrorCode.ISSUER_WALLET_NOT_FOUND))
                    .getWalletAddress();

            String treasuryAddress = walletRepository.findByWalletRole(WalletRole.PLATFORM_TREASURY)
                    .orElseThrow(() -> new BusinessException(ErrorCode.BUYER_WALLET_NOT_FOUND))
                    .getWalletAddress();

            StoToken stoToken = StoToken.deploy(
                    web3j,
                    issuerCredentials,
                    new DefaultGasProvider(),
                    token.getTokenName(),
                    token.getTokenSymbol(),
                    BigInteger.valueOf(token.getTotalSupply()),
                    BigInteger.valueOf(platformTokenHolding.getHoldingSupply()),
                    issuerAddress,
                    treasuryAddress
            ).send();

            String contractAddress = stoToken.getContractAddress();
            log.info("컨트랙트 배포 완료: {}", contractAddress);
            return contractAddress;
        } catch (Exception e) {
            log.error("컨트랙트 배포 실패", e);
            throw new BusinessException(ErrorCode.CONTRACT_DEPLOY_FAILED);
        }
    }
}

package server.main.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.ECKeyPair;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import server.main.global.util.WalletEncryptionUtil;
import server.main.member.entity.Wallet;
import server.main.member.entity.WalletRole;
import server.main.member.entity.WalletType;
import server.main.member.repository.WalletRepository;

import java.math.BigInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletAdminSetupService {

    @Value("${blockchain.issuer-private-key:}")
    private String issuerPrivateKey;

    private final WalletRepository walletRepository;
    private final WalletEncryptionUtil walletEncryptionUtil;

    @Transactional
    public void createIssuerWalletIfAbsent() {
        if (walletRepository.existsByWalletRole(WalletRole.ISSUER)) {
            log.info("ISSUER 지갑이 존재함");
            return;
        }

        if (issuerPrivateKey.isBlank()) {
            log.warn("blockchain.issuer-private-key 미설정");
            return;
        }

        String address = deriveAddress(issuerPrivateKey);
        walletRepository.save(Wallet.createForIssuer(address));
        log.info("ISSUER 지갑 생성 완료: {}", address);
    }



    @Transactional
    public void createTreasuryIfAbsent() {
        if (walletRepository.existsByWalletRole(WalletRole.PLATFORM_TREASURY)) {
            log.info("PLATFORM_TREASURY 지갑이 존재함");
            return;
        }

        try {
            var keyPair = Keys.createEcKeyPair();
            String privateKey = Numeric.toHexStringNoPrefix(keyPair.getPrivateKey());
            String address = "0x" + Keys.getAddress(keyPair);
            String encryptedPrivateKey = walletEncryptionUtil.encrypt(privateKey);

            walletRepository.save(Wallet.createForTreasury(address, encryptedPrivateKey));
            log.info("PLATFORM_TREASURY 지갑 생성: {}", address);
        } catch (Exception e) {
            throw new IllegalStateException("Treasury 지갑 생성에 실패했습니다.", e);
        }
    }

    private String deriveAddress(String privateKeyHex) {
        String hex = privateKeyHex.startsWith("0x") ? privateKeyHex.substring(2) : privateKeyHex;
        ECKeyPair keyPair = ECKeyPair.create(new BigInteger(hex, 16));
        return "0x" + Keys.getAddress(keyPair);
    }
}

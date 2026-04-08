package server.main.member.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.web3j.crypto.Keys;
import org.web3j.utils.Numeric;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.util.WalletEncryptionUtil;
import server.main.member.entity.Member;
import server.main.member.entity.Wallet;
import server.main.member.repository.WalletRepository;

import java.security.Key;

@Slf4j
@Service
@RequiredArgsConstructor
public class CustodiaWalletService {

    private final WalletRepository walletRepository;
    private final WalletEncryptionUtil walletEncryptionUtil;

    @Transactional
    public Wallet createMemberWallet(Member member) {

        try {
            var keyPair = Keys.createEcKeyPair();
            String privateKey = Numeric.toHexStringNoPrefix(keyPair.getPrivateKey());
            String address = "0x" + Keys.getAddress(keyPair);
            String encryptedPrivateKey = walletEncryptionUtil.encrypt(privateKey);

            Wallet wallet = Wallet.createForMember(member, address, encryptedPrivateKey);
            return walletRepository.save(wallet);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.WALLET_CREATION_FAILED);
        }
    }
}

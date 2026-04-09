package server.main.member.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.main.global.util.BaseEntity;

@Entity
@Getter
@Table(name = "WALLETS")
@NoArgsConstructor
public class Wallet extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wallet_id")
    private Long wallet_id;

    @Column(name = "wallet_address", nullable = false, unique = true)
    private String walletAddress;

    @Enumerated(EnumType.STRING)
    private WalletType walletType;

    @Enumerated(EnumType.STRING)
    private WalletStatus walletStatus;

    @Enumerated(EnumType.STRING)
    private WalletRole walletRole;

    @Column(name = "encrypted_private_key")
    private String encryptedPrivateKey;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    public static Wallet createForMember(Member member, String walletAddress, String encryptedPrivateKey) {
        Wallet wallet = new Wallet();
        wallet.member = member;
        wallet.walletAddress = walletAddress;
        wallet.encryptedPrivateKey = encryptedPrivateKey;
        wallet.walletType = WalletType.CUSTODIAL;
        wallet.walletStatus = WalletStatus.ACTIVE;
        wallet.walletRole = WalletRole.MEMBER;
        return wallet;
    }

    public static Wallet createForTreasury(String walletAddress, String encryptedPrivateKey) {
        Wallet wallet = new Wallet();
        wallet.walletAddress = walletAddress;
        wallet.encryptedPrivateKey = encryptedPrivateKey;
        wallet.walletType = WalletType.CUSTODIAL;
        wallet.walletStatus = WalletStatus.ACTIVE;
        wallet.walletRole = WalletRole.PLATFORM_TREASURY;
        return wallet;
    }
    public static Wallet createForIssuer(String walletAddress) {
        Wallet wallet = new Wallet();
        wallet.walletAddress = walletAddress;
        wallet.walletType = WalletType.CUSTODIAL;
        wallet.walletStatus = WalletStatus.ACTIVE;
        wallet.walletRole = WalletRole.ISSUER;
        return wallet;
    }


}

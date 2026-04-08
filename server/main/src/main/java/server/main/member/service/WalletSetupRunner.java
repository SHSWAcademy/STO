package server.main.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class WalletSetupRunner implements ApplicationRunner {

    private final WalletAdminSetupService walletAdminSetupService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        walletAdminSetupService.createIssuerWalletIfAbsent();;
        walletAdminSetupService.createTreasuryIfAbsent();
    }
}

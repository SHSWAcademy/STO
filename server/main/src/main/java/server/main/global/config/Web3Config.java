package server.main.global.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Slf4j
@Configuration
public class Web3Config {

    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.issuer-private-key:}")
    private String issuerPrivateKey;

    private Web3j web3jInstance;


    @Bean
    public Credentials issuerCredentials() {
        return Credentials.create(issuerPrivateKey);
    }

    @Bean
    public Web3j web3j() {
        if (web3jInstance == null) {
            web3jInstance = Web3j.build(new HttpService(rpcUrl));
        }
        return web3jInstance;
    }
}

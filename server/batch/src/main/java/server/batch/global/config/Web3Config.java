package server.batch.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;

@Configuration
public class Web3Config {
    @Value("${blockchain.rpc-url}")
    private String rpcUrl;

    @Value("${blockchain.issuer-private-key}")
    private String issuerPrivateKey;

    @Bean
    public Web3j web3j() {
        return Web3j.build(new HttpService(rpcUrl));
    }

    @Bean
    public Credentials issuerCredentials() {
        return Credentials.create(issuerPrivateKey);
    }

}

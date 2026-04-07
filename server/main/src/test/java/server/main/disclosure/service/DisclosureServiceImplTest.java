package server.main.diclosure.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "jwt.secret=test-jwt-secret-key-for-disclosure-service-test",
        "jwt.access-token-expiration=3600000"
})
class DisclosureServiceImplTest {

    @Autowired
    private DisclosureService disclosureService;

    // 공시 자동등록 테스트
    @Test
    void testRegisterDisclosure() {
        disclosureService.registerAssetDisclosure("서울빌딩", 5L);
    }
}
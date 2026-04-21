package server.main.disclosure.service;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=dGVzdHNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlc29ubHkzMmJ5dGVz",
        "jwt.access-token-expiration=3600000"
})
class DisclosureServiceImplTest {

    @Autowired
    private DisclosureService disclosureService;

    // 공시 자동등록 테스트
    @Test
    @Disabled("Requires seeded asset data in the integration database")
    void testRegisterDisclosure() {
        disclosureService.registerAssetDisclosure("서울빌딩", 5L);
    }
}

package server.main;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"jwt.secret=dGVzdHNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlc29ubHkzMmJ5dGVz",
		"jwt.access-token-expiration=3600000"
})
class MainApplicationTests {

	@Test
	void contextLoads() {
	}

}

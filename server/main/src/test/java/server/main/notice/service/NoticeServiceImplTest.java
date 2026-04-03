package server.main.notice.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.notice.entity.Notice;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "jwt.secret=dGVzdHNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlc29ubHkzMmJ5dGVz",
        "jwt.access-token-expiration=3600000"
})
class NoticeServiceImplTest {

    @Autowired
    private NoticeService noticeService;

    // 공지 자동등록 테스트
    @Test
    void testRegisterNotice() {
        AssetRegisterRequestDTO dto = AssetRegisterRequestDTO.builder()
                .assetAddress("서울시 마포구 올림픽로 어쩌고 저쩌고")
                .imgUrl("/")
                .tokenSymbol("s")
                .totalSupply(1000L)
                .assetName("서울빌딩")
                .initPrice(500L)
                .totalValue(500000000L)
                .isAllocated(true)
                .circulatingSupply(800L)
                .holdingSupply(200L)
                .build();
        noticeService.registerAssetNotice(dto);
    }
}
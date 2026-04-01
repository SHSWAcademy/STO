package server.main.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.asset.repository.AssetRepository;
import server.main.token.repository.TokenRepository;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminServiceImplTest {

    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private AdminService adminService;

    // 자산등록 테스트
    @Test
    void testInsetAsset() {
        AssetRegisterRequestDTO dto = AssetRegisterRequestDTO.builder()
                .assetAddress("서울시 마포구 올림픽로 어쩌고 저쩌고")
                .imgUrl("/")
                .tokenSymbol("s")
                .totalSupply(1000L)
                .assetName("서울빙딩")
                .initPrice(500L)
                .totalValue(500000000L)
                .isAllocated(true)
                .holdingSupply(200L)
                .build();

        adminService.registerAsset(dto);
    }
}
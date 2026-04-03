package server.main.admin.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.AssetDetailResponseDTO;
import server.main.admin.dto.AssetListResponseDTO;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.asset.repository.AssetRepository;
import server.main.token.repository.TokenRepository;

import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "jwt.secret=dGVzdHNlY3JldGtleWZvcnRlc3RpbmdwdXJwb3Nlc29ubHkzMmJ5dGVz",
        "jwt.access-token-expiration=3600000"
})
class AdminServiceImplTest {

    @Autowired
    private TokenRepository tokenRepository;
    @Autowired
    private AssetRepository assetRepository;
    @Autowired
    private AdminService adminService;

    // 자산등록 테스트
    @Test
    void testInsertAsset() throws IOException {
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
        // 가짜 이미지 파일
        MockMultipartFile imageFile = new MockMultipartFile(
                "imageFile",
                "test.jpg",
                "image/jpeg",
                "fake image content".getBytes()
        );
        // 가짜 PDF 파일
        MockMultipartFile pdfFile = new MockMultipartFile(
                "pdfFile",
                "test.pdf",
                "application/pdf",
                "fake pdf content".getBytes()
        );
        adminService.registerAsset(dto, imageFile, pdfFile);
    }

    // 자산상세조회 테스트
    @Test
    void testSelectAsset() {
        AssetDetailResponseDTO dto = adminService.getAssetDetail(5L);
        System.out.println("상세 조회 내역 test : " + dto);
    }

    // 자산 조회 리스트
    @Test
    void testSelectList() {
        List<AssetListResponseDTO> list =  adminService.getAssetList();
        System.out.println("자산 리스트 확인 : " + list);
    }
}
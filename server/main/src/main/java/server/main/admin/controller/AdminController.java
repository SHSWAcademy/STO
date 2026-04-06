package server.main.admin.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.AssetDetailResponseDTO;
import server.main.admin.dto.AssetListResponseDTO;
import server.main.admin.dto.AssetRegisterRequestDTO;
import server.main.admin.dto.AssetUpdateRequestDTO;
import server.main.admin.service.AdminService;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/admin/")
@Log4j2
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // 자산 등록 요청
    @PostMapping("/asset")
    public ResponseEntity<Void> registerAsset(@RequestPart AssetRegisterRequestDTO dto,
                                              @RequestPart MultipartFile imageFile,
                                              @RequestPart MultipartFile pdfFile) {
        adminService.registerAsset(dto, imageFile, pdfFile);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 자산 리스트 조회
    @GetMapping("/asset")
    public ResponseEntity <List<AssetListResponseDTO>>  getAssetList() {
        List<AssetListResponseDTO> list = adminService.getAssetList();
        return ResponseEntity.ok(list);
    }

    // 자산 상세조회
    @GetMapping("/asset/{assetId}")
    public ResponseEntity<AssetDetailResponseDTO> getAssetDetail(@PathVariable Long assetId) {
        AssetDetailResponseDTO dto = adminService.getAssetDetail(assetId);
        return ResponseEntity.ok(dto);
    }

    // 자산 수정
    // 수정 대상 : 자산명, 자산주소, 자산PDF, 자산 이미지, 토큰 심볼, 토큰 상태
    @PatchMapping("/asset/{assetId}")
    public ResponseEntity<Void> assetUpdate(
            @PathVariable Long assetId,
            @RequestPart AssetUpdateRequestDTO dto,
            @RequestPart(required = false) MultipartFile imageFile,
            @RequestPart(required = false) MultipartFile pdfFile) {
            // 수정 서비스 호출
            adminService.updateAsset(assetId, dto, imageFile, pdfFile);
        return ResponseEntity.ok().build();
    }

    // 발행 -> 발행완료상태 -> 오전9시 발행완료 -> 거래 중

 }

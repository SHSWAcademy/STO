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
 }

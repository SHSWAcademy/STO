package server.main.admin.controller;

import org.springframework.web.bind.annotation.RequestBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.main.admin.dto.*;
import server.main.admin.entity.Common;
import server.main.admin.service.AdminService;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
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

    // 배당 스케줄 등록
    @PostMapping("/allocationEvent")
    public ResponseEntity<Void> registerAllocationEvent(@RequestPart AllocationRegisterRequestDTO dto,
                                    @RequestPart MultipartFile file) {
        adminService.registerAllocation(dto, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 배당 리스트 조회
    @GetMapping("/allocationEvent")
    public ResponseEntity <List<AllocationListResponseDTO>> getAllocationEventList() {
        List<AllocationListResponseDTO> list = adminService.getAllocationList();
        return ResponseEntity.ok(list);
    }

    // 배당 스케줄 상세 조회
    @GetMapping("/allocationEvent/{allocationEventId}")
    public ResponseEntity<List<AllocationDetailResponseDTO>> getAllocationEventDetail(@PathVariable Long allocationEventId) {
        List<AllocationDetailResponseDTO> list = adminService.getAllocationDetailList(allocationEventId);
        return ResponseEntity.ok(list);
    }

    // 배당 수정 (미사용 수정금지)
    @PatchMapping("/allocationEvent/{allocationEventId}")
    public ResponseEntity<Void> updateAllocationEvent(@PathVariable Long allocationEventId,
                                                      @RequestPart AllocationUpdateRequestDTO dto,
                                                      @RequestPart MultipartFile file) {
        // 수정 서비스 호출
        adminService.updateAllocation(allocationEventId, dto, file);
        return ResponseEntity.ok().build();
    }

    // 플랫폼 기초 설정 등록 및 수정
    @PostMapping("/common")
    public ResponseEntity<Void> registerAndUpdateCommon(@RequestBody CommonDTO dto) {
        log.info("시스템 설정 파라미터: {}", dto);
        adminService.registerCommon(dto);
        return ResponseEntity.ok().build();
    }

    // 플랫폼 기초 설정 조회
    @GetMapping("/common")
    public ResponseEntity<CommonDTO> getCommon() {
        CommonDTO dto = adminService.getCommon();
        return ResponseEntity.ok(dto);
    }

 }

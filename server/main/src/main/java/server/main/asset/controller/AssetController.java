package server.main.asset.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import server.main.asset.dto.AssetDetailDto;
import server.main.asset.service.AssetService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class AssetController {

    private final AssetService assetService;

    @GetMapping("/main/{assetId}")
    public ResponseEntity<AssetDetailDto> assetDetails(@PathVariable Long assetId) {

        // 웹소켓 열기 로직 필요

        AssetDetailDto dto = assetService.getAssetDetail(assetId);

        return ResponseEntity.ok(dto);
    }
}

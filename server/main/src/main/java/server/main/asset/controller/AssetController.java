package server.main.asset.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.main.asset.dto.AssetMainResponseDto;
import server.main.asset.service.AssetService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset")
@Slf4j
public class AssetController {

    private final AssetService assetService;

    @GetMapping
    public ResponseEntity<List<AssetMainResponseDto>> getAssets(@RequestParam(defaultValue = "0") int page) {
        List<AssetMainResponseDto> dtos = assetService.getAssetsWith10Paging(page);

        return ResponseEntity.ok(dtos);
    }
}
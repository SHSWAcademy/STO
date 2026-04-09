package server.main.asset.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import server.main.asset.service.AssetService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/asset")
@Slf4j
public class AssetController {

    private final AssetService assetService;

}
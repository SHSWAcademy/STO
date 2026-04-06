package server.main.token.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.main.token.dto.TokenAllocationInfoResponseDto;
import server.main.token.dto.TokenAssetInfoResponseDto;
import server.main.token.dto.TokenChartDetailResponseDto;
import server.main.token.dto.TokenDisclosureResponseDto;
import server.main.token.service.TokenService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    // 토큰 (자산) 상세 조회 - '차트, 호가'
    @GetMapping("/{tokenId}/chart")
    public ResponseEntity<TokenChartDetailResponseDto> tokenChart(@PathVariable Long tokenId) {
        TokenChartDetailResponseDto dto = tokenService.getTokenDetail(tokenId);
        log.info("{}", dto);
        return ResponseEntity.ok(dto);
    }

    // 토큰 (자산) 상세 조회 - '종목 정보'
    @GetMapping("/{tokenId}/info")
    public ResponseEntity<TokenAssetInfoResponseDto> tokenAssetInfo(@PathVariable Long tokenId) {
        TokenAssetInfoResponseDto dto = tokenService.getTokenAssetInfo(tokenId);
        log.info("{}", dto);
        return ResponseEntity.ok(dto);
    }

    // 토큰 (자산) 상세 조회 - '배당금 내역'
    @GetMapping("/{tokenId}/allocation")
    public ResponseEntity<List<TokenAllocationInfoResponseDto>> allocationInfo(@PathVariable Long tokenId) {
        List<TokenAllocationInfoResponseDto> dto = tokenService.getAllocationInfo(tokenId);
        return ResponseEntity.ok(dto);
    }

    // 토큰 (자산) 상세 조회 - '공시'
    @GetMapping("/{tokenId}/disclosure")
    public ResponseEntity<List<TokenDisclosureResponseDto>> disclosureInfo(@PathVariable Long tokenId) {
        List<TokenDisclosureResponseDto> dtos = tokenService.getDisclosureInfo(tokenId);
        return ResponseEntity.ok(dtos);
    }
}

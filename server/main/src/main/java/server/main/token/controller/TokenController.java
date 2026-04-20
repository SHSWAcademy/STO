package server.main.token.controller;


import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.main.token.dto.SelectType;
import server.main.token.dto.*;
import server.main.token.service.TokenService;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
@Validated
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    // 토큰 (자산) 메인 페이지 (전체 조회)
    @GetMapping
    public ResponseEntity<List<TokenMainResponseDto>> getAssets(@RequestParam(defaultValue = "0") @Min(0) int page,                     // 페이징 처리
                                                                @RequestParam(defaultValue = "BASIC") SelectType selectType,    // 조회 타입 : 기본값 '전체'
                                                                @RequestParam(defaultValue = "DAY") PeriodType periodType) {    // 기간 타입 : 기본값 '1일'
        List<TokenMainResponseDto> dtos = tokenService.getTokenAssetsWith10Paging(page, selectType, periodType);
        return ResponseEntity.ok(dtos);
    }

    // 스파크라인 전용 조회 (기간 전환 시 토큰 목록 유지하고 스파크라인만 갱신)
    @GetMapping("/sparkline")
    public ResponseEntity<Map<Long, List<SparkPointDto>>> getSparklines(
            @RequestParam List<Long> tokenIds,
            @RequestParam(defaultValue = "DAY") PeriodType periodType) {
        return ResponseEntity.ok(tokenService.getSparklines(tokenIds, periodType));
    }

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

    // 호가 단위 조회
    @GetMapping("/{tokenId}/tick-size")
    public ResponseEntity<Long> getTickSize(@PathVariable Long tokenId) {
        return ResponseEntity.ok(tokenService.getTickSize(tokenId));
    }
}

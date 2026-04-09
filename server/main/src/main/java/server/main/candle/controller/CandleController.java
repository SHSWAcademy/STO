package server.main.candle.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.entity.CandleType;
import server.main.candle.service.CandleService;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
public class CandleController {
    private final CandleService candleService;

    @GetMapping("/{tokenId}/candle")
    public ResponseEntity<List<CandleResponseDto>> getCandleMinutes (@PathVariable Long tokenId, @RequestParam CandleType type) {
        List<CandleResponseDto> dtos = candleService.getCandles(tokenId, type);

        return ResponseEntity.ok(dtos);
    }
}

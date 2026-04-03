package server.main.trade.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import server.main.trade.dto.TradeResponseDto;
import server.main.trade.service.TradeService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping("/api/token/{tokenId}/trades")
    public ResponseEntity<List<TradeResponseDto>> getTrades(@PathVariable Long tokenId) {
        List<TradeResponseDto> dtos = tradeService.getTrades(tokenId);

        return ResponseEntity.ok(dtos);
    }
}

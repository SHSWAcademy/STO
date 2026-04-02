package server.main.order.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.main.order.dto.OrderRequestDto;
import server.main.order.service.OrderService;
import server.main.token.dto.TokenDetailDto;
import server.main.token.service.TokenService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/token")
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final TokenService tokenService;

    @GetMapping("/{tokenId}")
    public ResponseEntity<TokenDetailDto> tokenDetails(@PathVariable Long tokenId) {
        TokenDetailDto dto = tokenService.getTokenDetail(tokenId);
        log.info(String.valueOf(dto));
        return ResponseEntity.ok(dto);
    }

    // 매수, 매도 요청
    @PostMapping("/{tokenId}/order")
    public ResponseEntity<Void> order(@PathVariable Long tokenId, @Validated @RequestBody OrderRequestDto dto) {
        orderService.createOrder(tokenId, dto);
        return ResponseEntity.status(HttpStatus.OK).build();
    }
}

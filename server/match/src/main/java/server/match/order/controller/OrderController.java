package server.match.order.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.match.order.dto.MatchOrderRequestDto;
import server.match.order.dto.MatchResultDto;

import java.util.List;

@RestController
@RequestMapping("/internal/orders")
@RequiredArgsConstructor
public class OrderController {

    // TODO: 매칭 서비스 주입 예정

    @PostMapping
    public ResponseEntity<MatchResultDto> order(@RequestBody MatchOrderRequestDto dto) {
        // TODO: 실제 매칭 로직 구현 예정
        // 현재는 계약 정의 단계이므로 체결 없음(OPEN) 상태로 반환
        MatchResultDto result = MatchResultDto.builder()
                .orderId(dto.getOrderId())
                .tokenId(dto.getTokenId())
                .finalStatus(server.match.order.entity.OrderStatus.OPEN)
                .filledQuantity(0L)
                .remainingQuantity(dto.getOrderQuantity())
                .executions(List.of())
                .build();

        return ResponseEntity.ok(result);
    }
}

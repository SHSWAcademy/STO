package server.main.order.service;

import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;
import server.main.order.dto.UpdateOrderRequestDto;

import java.util.List;

public interface OrderService {

    // Phase 1: 검증 + 잔고 차감 + 주문 저장 → 커밋
    MatchOrderRequestDto validateAndSaveOrder(Long tokenId, OrderRequestDto dto);

    // Phase 2: 체결 결과 반영 + 이벤트 발행 → 커밋
    void processMatchResult(Long orderId, Long tokenId, MatchResultDto matchResult);

    // match 실패 시 보상 트랜잭션: 잔고 복구 + 주문 취소
    void compensateFailedOrder(Long orderId);

    // updateOrder Phase 1: 검증 + 잔고 재조정 + 주문 수정 → 커밋
    UpdateMatchOrderRequestDto validateAndUpdateOrder(Long orderId, UpdateOrderRequestDto dto);

    // updateOrder Phase 2: processMatchResult 재사용

    // match 실패 시 보상 (수정): 원래 상태로 복구
    void compensateFailedUpdate(Long orderId, Long originalPrice, Long originalQuantity);

    // 미체결 주문 조회
    List<PendingOrderResponseDto> getPendingOrders(Long tokenId);

    // cancelOrder Phase 1: 검증 + 잔고 복구 + PENDING 전환 → 커밋
    CancelOrderContext validateAndCancelOrder(Long orderId);

    // cancelOrder Phase 2: CANCELLED 최종 전환 → 커밋
    void completeCancelOrder(Long orderId);

    // cancel match 실패 시 보상: 잔고 재잠금 + 상태 복원
    void compensateFailedCancel(CancelOrderContext ctx);

    // 주문 가능 금액/수량 조회
    OrderCapacityResponseDto getOrderCapacity(Long tokenId);
    // 대기창 - 매수, 매도 요청 취소
    void cancelOrder(Long orderId);

    // 10%, 25%, 50%, 최대 주문 가능 금액/수량 조회
    OrderCapacityResponseDto getOrderCapacity(Long tokenId);
}

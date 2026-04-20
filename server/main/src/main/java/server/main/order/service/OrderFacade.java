package server.main.order.service;

import static server.main.global.error.ErrorCode.*;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.main.global.error.BusinessException;
import server.main.global.util.MatchClient;
import server.main.order.dto.CancelOrderContext;
import server.main.order.dto.CancelOrderRequestDto;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.MatchResultDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.dto.UpdateMatchOrderRequestDto;
import server.main.order.dto.UpdateOrderRequestDto;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFacade {

    private final OrderService orderService;
    private final MatchClient matchClient;

    public void createOrder(Long tokenId, OrderRequestDto dto) {
        MatchOrderRequestDto matchDto = orderService.validateAndSaveOrder(tokenId, dto);

        MatchResultDto matchResult;
        try {
            matchResult = matchClient.sendOrder(matchDto);
        } catch (RestClientException e) {
            log.error("match 서버 호출 실패. orderId={}", matchDto.getOrderId(), e);
            orderService.compensateFailedOrder(matchDto.getOrderId());
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        try {
            orderService.processMatchResult(matchDto.getOrderId(), tokenId, matchResult);
        } catch (RuntimeException e) {
            log.error("match Phase 2 ?ㅽ뙣. orderId={}", matchDto.getOrderId(), e);
            orderService.markOrderFailed(matchDto.getOrderId());
            throw e;
        }
    }

    public void updateOrder(Long orderId, UpdateOrderRequestDto dto) {
        UpdateMatchOrderRequestDto matchDto = orderService.validateAndUpdateOrder(orderId, dto);

        MatchResultDto matchResult;
        try {
            matchResult = matchClient.updateOrder(matchDto);
        } catch (RestClientException e) {
            log.error("match 서버 호출 실패. orderId={}", orderId, e);
            orderService.compensateFailedUpdate(orderId, matchDto.getOriginalPrice(), matchDto.getOriginalQuantity());
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        try {
            orderService.processMatchResult(orderId, matchDto.getTokenId(), matchResult);
        } catch (RuntimeException e) {
            log.error("updateOrder Phase 2 ?ㅽ뙣. orderId={}", orderId, e);
            orderService.markOrderFailed(orderId);
            throw e;
        }
    }

    public void cancelOrder(Long orderId, CancelOrderRequestDto dto) {
        CancelOrderContext ctx = orderService.validateAndCancelOrder(orderId, dto);

        try {
            matchClient.cancelOrder(ctx.getOrderId(), ctx.getTokenId());
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("match 서버에 취소 대상 주문이 없어 취소 완료로 정리합니다. orderId={}", orderId, e);
            orderService.completeCancelOrder(orderId);
            return;
        } catch (RestClientException e) {
            log.error("match 서버 호출 실패. orderId={}", orderId, e);
            orderService.compensateFailedCancel(ctx);
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        orderService.completeCancelOrder(orderId);
    }
}

package server.main.order.service;

import static server.main.global.error.ErrorCode.MATCH_SERVICE_UNAVAILABLE;

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
            log.error("Match service call failed. orderId={}", matchDto.getOrderId(), e);
            orderService.compensateFailedOrder(matchDto.getOrderId());
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        try {
            orderService.processMatchResult(matchDto.getOrderId(), tokenId, matchResult);
        } catch (RuntimeException e) {
            log.error("Match phase 2 failed. orderId={}", matchDto.getOrderId(), e);
            orderService.markOrderFailed(matchDto.getOrderId(), matchResult);
            throw e;
        }
    }

    public void updateOrder(Long orderId, UpdateOrderRequestDto dto) {
        UpdateMatchOrderRequestDto matchDto = orderService.validateAndUpdateOrder(orderId, dto);

        MatchResultDto matchResult;
        try {
            matchResult = matchClient.updateOrder(matchDto);
        } catch (RestClientException e) {
            log.error("Match service call failed. orderId={}", orderId, e);
            orderService.compensateFailedUpdate(orderId, matchDto.getOriginalPrice(), matchDto.getOriginalQuantity());
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        try {
            orderService.processMatchResult(orderId, matchDto.getTokenId(), matchResult);
        } catch (RuntimeException e) {
            log.error("Update order phase 2 failed. orderId={}", orderId, e);
            orderService.markOrderFailed(orderId, matchResult);
            throw e;
        }
    }

    public void cancelOrder(Long orderId, CancelOrderRequestDto dto) {
        CancelOrderContext ctx = orderService.validateAndCancelOrder(orderId, dto);

        try {
            matchClient.cancelOrder(ctx.getOrderId(), ctx.getTokenId());
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Cancel target was not found on match service. Marking as cancelled. orderId={}", orderId, e);
            orderService.completeCancelOrder(orderId);
            return;
        } catch (RestClientException e) {
            log.error("Match service call failed. orderId={}", orderId, e);
            orderService.compensateFailedCancel(ctx);
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        orderService.completeCancelOrder(orderId);
    }
}

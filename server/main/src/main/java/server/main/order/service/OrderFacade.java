package server.main.order.service;

import static server.main.global.error.ErrorCode.*;

import org.springframework.stereotype.Component;
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
        // phase 1
        MatchOrderRequestDto matchDto = orderService.validateAndSaveOrder(tokenId, dto);

        // match 호출
        MatchResultDto matchResult;
        try {
            matchResult = matchClient.sendOrder(matchDto);
        } catch (RestClientException e) {
            log.error("match 서버 호출 실패. orderId={}", matchDto.getOrderId(), e);
            orderService.compensateFailedOrder(matchDto.getOrderId());
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        // phase 2
        orderService.processMatchResult(matchDto.getOrderId(), tokenId, matchResult);
    }
    
    public void updateOrder(Long orderId, UpdateOrderRequestDto dto) {
        
        // phase 1
        UpdateMatchOrderRequestDto matchDto = orderService.validateAndUpdateOrder(orderId, dto);
        
        // match
        MatchResultDto matchResult;
        try {
            matchResult = matchClient.updateOrder(matchDto);
        } catch (RestClientException e){
            log.error("match 서버 호출 실패. orderId={}", orderId, e);
            orderService.compensateFailedUpdate(orderId, matchDto.getOriginalPrice(), matchDto.getOriginalQuantity());
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        // phase 2
        orderService.processMatchResult(orderId, matchDto.getTokenId(), matchResult);
    }

    public void cancelOrder(Long orderId, CancelOrderRequestDto dto) {
        // phase 1
        CancelOrderContext ctx = orderService.validateAndCancelOrder(orderId, dto);

        // match 호출
        try {
            matchClient.cancelOrder(ctx.getOrderId(), ctx.getTokenId());
        } catch (RestClientException e) {
            log.error("match 서버 호출 실패. orderId={}", orderId, e);
            orderService.compensateFailedCancel(ctx);
            throw new BusinessException(MATCH_SERVICE_UNAVAILABLE);
        }

        // phase 2
        orderService.completeCancelOrder(orderId);
    }
}

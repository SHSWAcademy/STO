package server.main.order.event;

import java.util.List;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import lombok.RequiredArgsConstructor;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.mapper.OrderMapper;
import server.main.order.repository.OrderRepository;

@Component
@RequiredArgsConstructor
public class OrderWebSocketEventListener {

    private final SimpMessagingTemplate messagingTemplate;
    private final OrderRepository orderRepository;
    private final OrderMapper orderMapper;

    // 트랜잭션 커밋 후 실행 — DB에 확실히 반영된 데이터만 push
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderWebSocketEvent(OrderWebSocketEvent event) {
        Long tokenId = event.getTokenId();
        Long memberId = event.getMemberId();

        // 현재 사용자 대기 주문 push
        List<PendingOrderResponseDto> myPending =
                orderMapper.toPendingDtoList(orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId));
        messagingTemplate.convertAndSend("/topic/pendingOrders/" + tokenId + "/" + memberId, myPending);

        // 상대방 대기 주문 push
        for (Long counterMemberId : event.getCounterMemberIds()) {
            List<PendingOrderResponseDto> counterPending =
                    orderMapper.toPendingDtoList(orderRepository.findPendingOrderByMemberAndToken(counterMemberId, tokenId));
            messagingTemplate.convertAndSend("/topic/pendingOrders/" + tokenId + "/" + counterMemberId, counterPending);
        }
    }
}

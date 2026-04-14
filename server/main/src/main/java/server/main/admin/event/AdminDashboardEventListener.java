package server.main.admin.event;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.main.admin.service.AdminService;

@Component
@RequiredArgsConstructor
public class AdminDashboardEventListener {
    private final SimpMessagingTemplate messagingTemplate;
    private final AdminService adminService;

    // 상단 모든 데이터 이벤트 발행
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSummary(AdminDashboardEvent event) {
        messagingTemplate.convertAndSend("/topic/admin/dashboard",
                adminService.getDashBoard());
    }

    // 체결 내역 실시간 조회
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTrades(TradeExecutedEvent event) {
          messagingTemplate.convertAndSend("/topic/admin/trades", event.getDashBoardTradeListDTO());
    }
}

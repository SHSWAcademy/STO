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

    // 모든 이벤트 → summary push
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSummary(AdminDashboardEvent event) {
//        messagingTemplate.convertAndSend("/topic/admin/dashboard",
//                adminService.getDashBoard());
    }

    // 체결 이벤트 → 거래 완료 최신 5건 push
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTrades(TradeExecutedEvent event) {
//        messagingTemplate.convertAndSend("/topic/admin/trades",
//                adminService.getLatestTrades(5));
    }
}

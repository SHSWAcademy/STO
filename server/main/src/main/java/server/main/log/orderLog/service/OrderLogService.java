package server.main.log.orderLog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.main.log.orderLog.entity.OrderLog;
import server.main.log.orderLog.repository.OrderLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderLogService {

    private final OrderLogRepository orderLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String memberName,String orderType, String detail, boolean result) {
        orderLogRepository.save(OrderLog.builder()
                .timeStamp(LocalDateTime.now())
                .identifier(memberName)
                .orderType(orderType)
                .task("ORDER_CREATE")
                .detail(detail)
                .result(result)
                .build());
    }

    // 주문요청 로그 조회 (admin)
    public Page<OrderLog> findOrderLog(Pageable pageable) {
        return orderLogRepository.findAll(pageable);
    }
}
package server.main.order.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FailedOrderRetryScheduler {

    private final OrderService orderService;

    @Scheduled(
            fixedDelayString = "${order.retry.failed.fixed-delay-ms:60000}",
            initialDelayString = "${order.retry.failed.initial-delay-ms:60000}"
    )
    public void retryFailedOrders() {
        orderService.retryFailedOrders();
    }
}

package server.match.order;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import server.match.order.entity.OrderType;
import server.match.order.model.Order;
import server.match.order.service.OrderBookRegistry;

@Component
@RequiredArgsConstructor
public class OrderBookInitializer implements ApplicationRunner {
    // implements ApplicationRunner <-> @PostConstruct
    // 매치 서버가 처음 시작될 때 DB 호가창 정보를 불러와 인메모리 호가창에 반영

    // match 서버 (OrderBookInitializer) — 서버 시작 시 DB 조회 -> 매치 in-memory에 적재
    // main 서버 (OrderBookSubscribeHandler) — 상세 페이지 접근 시 DB 조회 -> 화면에 스냅샷 전송
    // 각각 DB 조회

    private final JdbcTemplate jdbcTemplate;
    private final OrderBookRegistry orderBookRegistry;

    @Override
    public void run(ApplicationArguments args) {
        String sql = """
                SELECT order_id, member_id, token_id, order_type, order_price, remaining_quantity
                FROM orders
                WHERE order_status IN ('OPEN', 'PARTIAL')
                """;

        jdbcTemplate.query(sql, rs -> {
            Long orderId   = rs.getLong("order_id");
            Long memberId  = rs.getLong("member_id");
            Long tokenId   = rs.getLong("token_id");
            OrderType type = OrderType.valueOf(rs.getString("order_type"));
            Long price     = rs.getLong("order_price");
            Long remaining = rs.getLong("remaining_quantity");

            Order order = new Order(orderId, memberId, tokenId, type, price, remaining);
            orderBookRegistry.getOrCreate(tokenId).addOrder(order);
        });
    }
}

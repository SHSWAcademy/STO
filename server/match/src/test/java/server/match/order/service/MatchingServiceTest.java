package server.match.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.match.global.redis.RedisPublisher;
import server.match.order.dto.MatchResultDto;
import server.match.order.entity.OrderStatus;
import server.match.order.entity.OrderType;
import server.match.order.model.Order;
import server.match.order.model.OrderBook;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    RedisPublisher redisPublisher;

    @InjectMocks
    MatchingService matchingService;

    @Test
    void buyOrder_matchesByPricePriority() {
        // Given : tokenId = 100L 인 OrderBook생성
        OrderBook orderBook = new OrderBook(100L);

        // 매도 주문 2개 - 가격이 다름
        Order cheapSell = new Order(1L, 10L, 100L, OrderType.SELL, 999L, 5L); // 999원
        Order expSell = new Order(2L, 11L, 100L, OrderType.SELL, 1001L, 5L); // 1001원
        orderBook.addOrder(cheapSell);
        orderBook.addOrder(expSell);

        // 매수 주문 - 가격 1000원, 수량 5
        Order buyOrder = new Order(3L, 20L, 100L, OrderType.BUY, 1000L, 5L);

        // When
        MatchResultDto result = matchingService.match(buyOrder, orderBook);

        // Then
        assertThat(result.getExecutions()).hasSize(1);
        assertThat(result.getExecutions().get(0).getTradePrice()).isEqualTo(999L);
        assertThat(result.getFinalStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void buyOrder_matchesByTimePriority() {
        // Given: 같은 가격(1000원)에 매도 주문 2개 — 먼저 들어온 순서가 먼저 체결돼야 함
        OrderBook orderBook = new OrderBook(100L);

        Order firstSell  = new Order(1L, 10L, 100L, OrderType.SELL, 1000L, 5L); // 먼저 등록
        Order secondSell = new Order(2L, 11L, 100L, OrderType.SELL, 1000L, 5L); // 나중에 등록
        orderBook.addOrder(firstSell);
        orderBook.addOrder(secondSell);

        // 매수 주문 — 수량 5 (firstSell 하나만 체결되는 수량)
        Order buyOrder = new Order(3L, 20L, 100L, OrderType.BUY, 1000L, 5L);

        // When
        MatchResultDto result = matchingService.match(buyOrder, orderBook);

        // Then: 먼저 등록된 firstSell(orderId=1)과 체결되어야 한다
        assertThat(result.getExecutions()).hasSize(1);
        assertThat(result.getExecutions().get(0).getCounterOrderId()).isEqualTo(1L);
        assertThat(result.getFinalStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void buyOrder_partialFill() {
        // Given: 매도 수량 3 < 매수 수량 10 → 3만 체결되고 7이 남아야 함
        OrderBook orderBook = new OrderBook(100L);

        Order sellOrder = new Order(1L, 10L, 100L, OrderType.SELL, 1000L, 3L); // 수량 3
        orderBook.addOrder(sellOrder);

        Order buyOrder = new Order(2L, 20L, 100L, OrderType.BUY, 1000L, 10L); // 수량 10

        // When
        MatchResultDto result = matchingService.match(buyOrder, orderBook);

        // Then
        assertThat(result.getFilledQuantity()).isEqualTo(3L);      // 체결된 수량
        assertThat(result.getRemainingQuantity()).isEqualTo(7L);   // 남은 수량
        assertThat(result.getFinalStatus()).isEqualTo(OrderStatus.PARTIAL);
    }

    @Test
    void buyOrder_fullFill() {
        // Given: 매도 수량 5 = 매수 수량 5 → 전량 체결
        OrderBook orderBook = new OrderBook(100L);

        Order sellOrder = new Order(1L, 10L, 100L, OrderType.SELL, 1000L, 5L);
        orderBook.addOrder(sellOrder);

        Order buyOrder = new Order(2L, 20L, 100L, OrderType.BUY, 1000L, 5L);

        // When
        MatchResultDto result = matchingService.match(buyOrder, orderBook);

        // Then
        assertThat(result.getFilledQuantity()).isEqualTo(5L);
        assertThat(result.getRemainingQuantity()).isEqualTo(0L);
        assertThat(result.getFinalStatus()).isEqualTo(OrderStatus.FILLED);
    }

    @Test
    void unmatchedOrder_remainsInOrderBook() {
        // Given: 매수 희망가 900원 < 매도 호가 1000원 → 가격 불일치로 체결 안 됨
        OrderBook orderBook = new OrderBook(100L);

        Order sellOrder = new Order(1L, 10L, 100L, OrderType.SELL, 1000L, 5L);
        orderBook.addOrder(sellOrder);

        Order buyOrder = new Order(2L, 20L, 100L, OrderType.BUY, 900L, 5L); // 900원으로 매수 시도

        // When
        MatchResultDto result = matchingService.match(buyOrder, orderBook);

        // Then: 체결 없이 매수 주문이 OrderBook에 잔류
        assertThat(result.getFilledQuantity()).isEqualTo(0L);
        assertThat(result.getFinalStatus()).isEqualTo(OrderStatus.OPEN);
        assertThat(orderBook.findById(2L)).isNotNull(); // buyOrder가 OrderBook에 남아 있어야 함
    }

    @Test
    void separateOrderBooks_doNotInteract() {
        // Given: 서로 다른 두 OrderBook 인스턴스는 독립적이어야 함
        OrderBook orderBookA = new OrderBook(100L); // 토큰 A
        OrderBook orderBookB = new OrderBook(200L); // 토큰 B

        Order sellOrder = new Order(1L, 10L, 100L, OrderType.SELL, 1000L, 5L);
        orderBookA.addOrder(sellOrder); // 토큰 A의 오더북에만 등록

        Order buyOrder = new Order(2L, 20L, 200L, OrderType.BUY, 1000L, 5L);

        // When: 토큰 B의 오더북으로 매칭
        MatchResultDto result = matchingService.match(buyOrder, orderBookB);

        // Then: 체결 없음
        assertThat(result.getFilledQuantity()).isEqualTo(0L);
        assertThat(result.getFinalStatus()).isEqualTo(OrderStatus.OPEN);

        // 오더북 격리 검증 — A와 B가 서로의 주문을 공유하지 않아야 함
        assertThat(orderBookA.findById(1L)).isNotNull(); // A의 매도 주문은 A에 그대로
        assertThat(orderBookB.findById(1L)).isNull();    // A의 매도 주문이 B에 없어야 함
        assertThat(orderBookB.findById(2L)).isNotNull(); // 미체결 buyOrder는 B에 등록됨
        assertThat(orderBookA.findById(2L)).isNull();    // buyOrder가 A에는 없어야 함
    }

    @Test
    void matchResult_dtoFieldsAreCorrect() {
        // Given: 매도 5개, 매수 10개 → 5개만 체결되고 매수 5개 잔량 (PARTIAL)
        OrderBook orderBook = new OrderBook(100L);

        Order sellOrder = new Order(1L, 10L, 100L, OrderType.SELL, 1000L, 5L);
        orderBook.addOrder(sellOrder);

        Order buyOrder = new Order(2L, 20L, 100L, OrderType.BUY, 1000L, 10L);

        // When
        MatchResultDto result = matchingService.match(buyOrder, orderBook);

        // Then: MatchResultDto 주요 필드 검증
        assertThat(result.getOrderId()).isEqualTo(2L);
        assertThat(result.getTokenId()).isEqualTo(100L);
        assertThat(result.getFilledQuantity()).isEqualTo(5L);
        assertThat(result.getRemainingQuantity()).isEqualTo(5L);
        assertThat(result.getFinalStatus()).isEqualTo(OrderStatus.PARTIAL);

        // TradeExecutionDto 필드 검증
        assertThat(result.getExecutions()).hasSize(1);
        assertThat(result.getExecutions().get(0).getCounterOrderId()).isEqualTo(1L);
        assertThat(result.getExecutions().get(0).getCounterMemberId()).isEqualTo(10L);
        assertThat(result.getExecutions().get(0).getTradePrice()).isEqualTo(1000L);
        assertThat(result.getExecutions().get(0).getTradeQuantity()).isEqualTo(5L);
    }
}

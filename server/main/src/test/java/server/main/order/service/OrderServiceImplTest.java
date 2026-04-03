package server.main.order.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import server.main.order.repository.OrderRepository;
import server.main.token.mapper.TokenMapper;
import server.main.token.repository.TokenRepository;
import server.main.token.service.TokenServiceImpl;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    TokenRepository tokenRepository;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OrderService orderService;

    @Test
    void createOrder() {
        //given


        //when


        //then

    }

    @Test
    void getPendingOrders() {
    }

    @Test
    void cancelOrder() {
    }
}
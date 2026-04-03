package server.main.order.service;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.global.error.BusinessException;
import server.main.global.security.CustomUserPrincipal;
import server.main.global.websocket.MatchClient;
import server.main.member.entity.Member;
import server.main.member.entity.MemberTokenHolding;
import server.main.member.repository.MemberRepository;
import server.main.member.repository.MemberTokenHoldingRepository;
import server.main.order.dto.MatchOrderRequestDto;
import server.main.order.dto.OrderRequestDto;
import server.main.order.entity.Order;
import server.main.order.entity.OrderType;
import server.main.order.repository.OrderRepository;
import server.main.token.entity.Token;
import server.main.token.repository.TokenRepository;

import java.util.List;

import static server.main.global.error.ErrorCode.*;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final TokenRepository tokenRepository;
    private final MemberRepository memberRepository;
    private final MemberTokenHoldingRepository memberTokenHoldingRepository;
    private final MatchClient matchClient;


    @Transactional
    @Override
    public void createOrder(Long tokenId, OrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = principal.getId();
        Member findMember = memberRepository.findById(memberId).orElseThrow(() -> new EntityNotFoundException("cannot find entity"));
        Token findToken = tokenRepository.findById(tokenId).orElseThrow(() -> new EntityNotFoundException("cannot find entity"));

        // 매수: 원화 잔고 >= 총 주문 금액 검증
        if (OrderType.BUY.equals(dto.getOrderType()) &&
                findMember.getAccount().getAvailableBalance() < dto.getOrderPrice() * dto.getOrderQuantity()) {
            throw new BusinessException(INSUFFICIENT_BALANCE);
        }

        // 매도: 보유 수량 >= 요청 수량 검증
        if (OrderType.SELL.equals(dto.getOrderType())) {
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findByMemberAndToken(findMember, findToken)
                    .orElseThrow(() -> new BusinessException(INSUFFICIENT_TOKEN_BALANCE));

            if (tokenHolding.getCurrentQuantity() < dto.getOrderQuantity())
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);
        }

        Order createOrder = Order.builder()
                .orderPrice(dto.getOrderPrice())
                .orderQuantity(dto.getOrderQuantity())
                .orderType(dto.getOrderType())
                .token(findToken)
                .member(findMember)
                .build();

        log.info(String.valueOf(createOrder));

        orderRepository.save(createOrder);

        // 매치 서버로 전달
        MatchOrderRequestDto matchDto = MatchOrderRequestDto.builder()
                .tokenId(tokenId)
                .memberId(memberId)
                .orderId(createOrder.getOrderId())
                .orderPrice(createOrder.getOrderPrice())
                .orderQuantity(createOrder.getOrderQuantity())
                .orderType(createOrder.getOrderType())
                .build();
        matchClient.sendOrder(matchDto); // sendOrder 실패 시 모두 롤백
    }

    @Override
    public List<?> getPendingOrders(Long tokenId) {
        return List.of();
    }

    @Override
    public void cancelOrder(Long orderId) {

    }
}

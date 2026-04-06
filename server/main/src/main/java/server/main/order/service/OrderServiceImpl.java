package server.main.order.service;

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
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.dto.UpdateOrderRequestDto;
import server.main.order.entity.Order;
import server.main.order.entity.OrderStatus;
import server.main.order.entity.OrderType;
import server.main.order.mapper.OrderMapper;
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

    private final OrderMapper orderMapper;
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
        Member findMember = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
        Token findToken = tokenRepository.findById(tokenId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));

        // 매수일 경우
        if (OrderType.BUY.equals(dto.getOrderType())) {
            // 원화 잔고 >= 총 주문 금액 검증
            if (findMember.getAccount().getAvailableBalance() < dto.getOrderPrice() * dto.getOrderQuantity())
                throw new BusinessException(INSUFFICIENT_BALANCE);

            // 매수 주문일 경우 구매력 차감 (current quantity 감소, locked quantity 증가)
            else findMember.getAccount().lockBalance(dto.getOrderPrice() * dto.getOrderQuantity()); // 더티 체킹 (별도 update 쿼리 날리지 않아도 된다)
        }


        // 매도일 경우
        if (OrderType.SELL.equals(dto.getOrderType())) {
            // 보유 수량 >= 요청 수량 검증
            MemberTokenHolding tokenHolding = memberTokenHoldingRepository
                    .findByMemberAndToken(findMember, findToken)
                    .orElseThrow(() -> new BusinessException(INSUFFICIENT_TOKEN_BALANCE));

            if (tokenHolding.getCurrentQuantity() < dto.getOrderQuantity())
                throw new BusinessException(INSUFFICIENT_TOKEN_BALANCE);

            // 매도 주문일 경우 보유 토큰 감소 (current quantity 감소, locked quantity 증가)
            tokenHolding.lockQuantity(dto.getOrderQuantity()); // 더티 체킹 (별도 update 쿼리 날리지 않아도 된다)
        }

        // 주문 생성 (매도, 매수)
        Order createOrder = Order.builder()
                .orderPrice(dto.getOrderPrice())
                .orderQuantity(dto.getOrderQuantity())
                .orderType(dto.getOrderType()) // 매도, 매수 판단
                .orderStatus(OrderStatus.OPEN)
                .token(findToken)
                .member(findMember)
                // oderSequence 는 match server 에서 채운다
                .build();

        log.info(String.valueOf(createOrder));

        // 주문 저장
        orderRepository.save(createOrder);

        // 주문을 매치 서버로 전달
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

    // 상세 화면의 '대기' : 소켓 필요
    @Override
    public List<PendingOrderResponseDto> getPendingOrders(Long tokenId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = principal.getId();

        List<Order> pendingOrders = orderRepository.findPendingOrderByMemberAndToken(memberId, tokenId);
        List<PendingOrderResponseDto> dtos = orderMapper.toPendingDtoList(pendingOrders);
        return dtos;
    }


    @Transactional
    @Override
    public void updateOrder(Long orderId, UpdateOrderRequestDto dto) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = principal.getId();
        Member findMember = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
    }

    @Transactional
    @Override
    public void cancelOrder(Long orderId) {
        CustomUserPrincipal principal = (CustomUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long memberId = principal.getId();
        Member findMember = memberRepository.findById(memberId).orElseThrow(() -> new BusinessException(ENTITY_NOT_FOUNT_ERROR));
    }

}
package server.main.token.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import server.main.asset.entity.QAsset;
import server.main.token.dto.SelectType;
import server.main.token.entity.QToken;
import server.main.token.entity.Token;
import server.main.token.entity.TokenStatus;
import server.main.trade.entity.QTrade;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public class TokenRepositoryImpl implements TokenRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    // selectType 에 맞게 동적으로 정렬, 페이징 처리하여 반환
    // BASIC - 1 토큰 당 currentPrice로 정렬
    // TOTAL_TRADE_VALUE - 토큰 별 전체 거래 금액으로 정렬
    // TOTAL_TRADE_QUANTITY - 토큰 별 전체 거래 수량으로 정렬
    @Override
    public List<Token> findAllBySelectType(int page, SelectType selectType) {
        QToken token = QToken.token;
        QAsset asset = QAsset.asset;
        QTrade trade = QTrade.trade;

        // BASIC: current_price 정렬 — 단순 페이징 (정렬 - 현재 가격으로 먼저 DESC 정렬, 현재 가격이 같을 경우 tokenId로 ASC 정렬)
        if (selectType == SelectType.BASIC) {
            return queryFactory
                    .selectFrom(token)
                    .join(token.asset, asset).fetchJoin()
                    .where(token.tokenStatus.eq(TokenStatus.TRADING))
                    .orderBy(token.currentPrice.desc(), token.tokenId.asc())
                    .offset((long) page * 10)
                    .limit(10)
                    .fetch();
        }

        // BASIC 이 아니라 TOTAL_TRADE_VALUE / TOTAL_TRADE_QUANTITY 일 경우
        // fetchJoin & groupBy는 JPA 제약으로 한 번에 불가 -> 두 단계로 처리 (groupBy랑 leftJoin은 한 번에 가능)

        // 1단계 : Token 과 Trade LEFT JOIN, groupBy 토큰 Id  — 거래 없는 토큰도 포함, null은 NullsLast (가장 마지막에 거래가 없는 null 토큰이 조회된다)
        OrderSpecifier<?> orderSpecifier = selectType == SelectType.TOTAL_TRADE_VALUE
                ? new OrderSpecifier<>(Order.DESC, trade.totalTradePrice.sum(), OrderSpecifier.NullHandling.NullsLast) // 총 거래 금액 정렬일 경우
                : new OrderSpecifier<>(Order.DESC, trade.tradeQuantity.sum(), OrderSpecifier.NullHandling.NullsLast);  // 총 거래 수량 정렬일 경우

        List<Long> tokenIds = queryFactory
                .select(token.tokenId)
                .from(token)
                .leftJoin(trade).on(trade.token.eq(token))
                .where(token.tokenStatus.eq(TokenStatus.TRADING))
                .groupBy(token.tokenId)
                .orderBy(orderSpecifier, token.tokenId.asc()) // 총 거래 금액 or 수량 DESC 정렬, 동일할 경우 tokenId ASC 정렬
                .offset((long) page * 10)
                .limit(10)
                .fetch();

        if (tokenIds.isEmpty()) return List.of();

        // 2단계: tokenId로 Token + Asset FETCH JOIN 조회
        List<Token> tokens = queryFactory
                .selectFrom(token)
                .join(token.asset, asset).fetchJoin()
                .where(token.tokenId.in(tokenIds)) // 1단계에서 추출한 tokenIds를 in 절로 넣는다
                .fetch();


        // in 절로 넣은 쿼리의 결과값들을 1단계 정렬 조건과 동일하게 다시 정렬
        Map<Long, Token> tokenMap = tokens.stream()
                .collect(Collectors.toMap(Token::getTokenId, t -> t));
        return tokenIds.stream()
                .map(tokenMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}

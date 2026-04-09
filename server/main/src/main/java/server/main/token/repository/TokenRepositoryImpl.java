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

    @Override
    public List<Token> findAllBySelectType(int page, SelectType selectType) {
        QToken token = QToken.token;
        QAsset asset = QAsset.asset;
        QTrade trade = QTrade.trade;

        // BASIC: current_price 정렬 — 단순 페이징
        if (selectType == SelectType.BASIC) {
            return queryFactory
                    .selectFrom(token)
                    .join(token.asset, asset).fetchJoin()
                    .where(token.tokenStatus.eq(TokenStatus.TRADING))
                    .orderBy(token.currentPrice.desc())
                    .offset((long) page * 10)
                    .limit(10)
                    .fetch();
        }

        // TOTAL_TRADE_VALUE / TOTAL_TRADE_QUANTITY
        // fetchJoin 랑 groupBy는 JPA 제약으로 불가, 두 단계로 처리
        // 1단계: Token 기준 LEFT JOIN — 거래 없는 토큰도 포함, null은 NullsLast
        OrderSpecifier<?> orderSpecifier = selectType == SelectType.TOTAL_TRADE_VALUE
                ? new OrderSpecifier<>(Order.DESC, trade.totalTradePrice.sum(), OrderSpecifier.NullHandling.NullsLast)
                : new OrderSpecifier<>(Order.DESC, trade.tradeQuantity.sum(), OrderSpecifier.NullHandling.NullsLast);

        List<Long> tokenIds = queryFactory
                .select(token.tokenId)
                .from(token)
                .leftJoin(trade).on(trade.token.eq(token))
                .groupBy(token.tokenId)
                .orderBy(orderSpecifier)
                .offset((long) page * 10)
                .limit(10)
                .fetch();

        if (tokenIds.isEmpty()) return List.of();

        // 2단계: tokenId로 Token + Asset fetchJoin 조회
        List<Token> tokens = queryFactory
                .selectFrom(token)
                .join(token.asset, asset).fetchJoin()
                .where(token.tokenId.in(tokenIds))
                .fetch();

        // 1단계 정렬 순서 복원
        Map<Long, Token> tokenMap = tokens.stream()
                .collect(Collectors.toMap(Token::getTokenId, t -> t));
        return tokenIds.stream()
                .map(tokenMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

    }
}

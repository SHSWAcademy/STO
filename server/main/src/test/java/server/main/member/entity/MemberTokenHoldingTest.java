package server.main.member.entity;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class MemberTokenHoldingTest {

    private MemberTokenHolding holding;

    @BeforeEach
    void setUp() {
        // 초기 상태: 수량 10, 평균 매수가 1000
        holding = MemberTokenHolding.createForBuyer(null, null, 10L, 1000L);
    }

    // createForBuyer
    @Test
    void createForBuyer_초기값_검증() {
        assertThat(holding.getCurrentQuantity()).isEqualTo(10L);
        assertThat(holding.getLockedQuantity()).isEqualTo(0L);
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    // settleBuyTrade — 평균 매수가 갱신
    @Test
    void settleBuyTrade_평균매수가_갱신() {
        // 보유: 10개 @ 1000 → 추가 매수: 10개 @ 2000
        // 신규 평균 = (1000*10 + 2000*10) / 20 = 1500
        holding.settleBuyTrade(10L, 2000L);

        assertThat(holding.getCurrentQuantity()).isEqualTo(20L);
        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("1500.0000"));
    }

    @Test
    void settleBuyTrade_동일가격_평균불변() {
        holding.settleBuyTrade(10L, 1000L);

        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("1000.0000"));
    }

    @Test
    void settleBuyTrade_낮은가격_평균감소() {
        // 보유: 10개 @ 1000 → 추가: 10개 @ 500
        // 신규 평균 = (1000*10 + 500*10) / 20 = 750
        holding.settleBuyTrade(10L, 500L);

        assertThat(holding.getAvgBuyPrice()).isEqualByComparingTo(new BigDecimal("750.0000"));
    }

    // settleSellTrade
    @Test
    void settleSellTrade_잠긴수량_차감() {
        holding.lockQuantity(5L);
        holding.settleSellTrade(5L);

        assertThat(holding.getLockedQuantity()).isEqualTo(0L);
        assertThat(holding.getCurrentQuantity()).isEqualTo(5L);
    }

    // lockQuantity
    @Test
    void lockQuantity_current_감소_locked_증가() {
        holding.lockQuantity(3L);

        assertThat(holding.getCurrentQuantity()).isEqualTo(7L);
        assertThat(holding.getLockedQuantity()).isEqualTo(3L);
    }

    // cancelOrder
    @Test
    void cancelOrder_잠긴수량_복구() {
        holding.lockQuantity(5L);
        holding.cancelOrder(5L);

        assertThat(holding.getCurrentQuantity()).isEqualTo(10L);
        assertThat(holding.getLockedQuantity()).isEqualTo(0L);
    }
}

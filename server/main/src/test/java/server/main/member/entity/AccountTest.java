package server.main.member.entity;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountTest {

    private Account account;

    @BeforeEach
    void setUp() {
        account = Account.create(null, "123-456", "pw");
    }

    // lockBalance
    @Test
    void lockBalance_구매력_차감() {
        account.lockBalance(1_000_000L);

        assertThat(account.getAvailableBalance()).isEqualTo(-1_000_000L);
        assertThat(account.getLockedBalance()).isEqualTo(1_000_000L);
    }

    // settleBuyTrade
    @Test
    void settleBuyTrade_주문가_체결가_동일_차액없음() {
        // 주문가=체결가=12000, 수량=5 → lockedAmount=60000, tradeAmount=60000
        // 순 비용 = 60000 (환급 없음)
        account.lockBalance(60_000L);
        long availableAfterLock = account.getAvailableBalance(); // -60000
        account.settleBuyTrade(60_000L, 60_000L);

        assertThat(account.getLockedBalance()).isEqualTo(0L);
        assertThat(account.getAvailableBalance()).isEqualTo(availableAfterLock); // 환급 없으므로 변화 없음
    }

    @Test
    void settleBuyTrade_주문가_높을때_차액_환급() {
        // 주문가 12000, 체결가 10000, 수량 5 → lockedAmount=60000, tradeAmount=50000
        // 차액 10000 환급 → 순 비용 = 50000
        account.lockBalance(60_000L);
        long availableBeforeLock = account.getAvailableBalance() + 60_000L; // lock 이전 기준
        account.settleBuyTrade(50_000L, 60_000L);

        assertThat(account.getLockedBalance()).isEqualTo(0L);
        assertThat(account.getAvailableBalance()).isEqualTo(availableBeforeLock - 50_000L); // 순 비용 50000
    }

    // settleSellTrade
    @Test
    void settleSellTrade_매도_대금_수령() {
        account.settleSellTrade(50_000L);

        assertThat(account.getAvailableBalance()).isEqualTo(50_000L);
    }

    // cancelOrder
    @Test
    void cancelOrder_잠긴금액_복구() {
        account.lockBalance(30_000L);
        account.cancelOrder(30_000L);

        assertThat(account.getAvailableBalance()).isEqualTo(0L);
        assertThat(account.getLockedBalance()).isEqualTo(0L);
    }

    // relockBalance
    @Test
    void relockBalance_주문수정_금액증가() {
        account.lockBalance(60_000L); // 기존 주문
        account.relockBalance(60_000L, 100_000L); // 수정: 더 큰 금액

        assertThat(account.getLockedBalance()).isEqualTo(100_000L);
        assertThat(account.getAvailableBalance()).isEqualTo(-100_000L);
    }

    @Test
    void relockBalance_주문수정_금액감소() {
        account.lockBalance(60_000L);
        account.relockBalance(60_000L, 30_000L);

        assertThat(account.getLockedBalance()).isEqualTo(30_000L);
        assertThat(account.getAvailableBalance()).isEqualTo(-30_000L);
    }
}

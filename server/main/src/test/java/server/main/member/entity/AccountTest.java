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

    // settleBuyTrade — 수수료 없음 (기존 로직 검증)
    @Test
    void settleBuyTrade_주문가_체결가_동일_차액없음() {
        account.lockBalance(60_000L);
        long availableAfterLock = account.getAvailableBalance();
        account.settleBuyTrade(60_000L, 60_000L, 0L);

        assertThat(account.getLockedBalance()).isEqualTo(0L);
        assertThat(account.getAvailableBalance()).isEqualTo(availableAfterLock);
    }

    @Test
    void settleBuyTrade_주문가_높을때_차액_환급() {
        account.lockBalance(60_000L);
        long availableBeforeLock = account.getAvailableBalance() + 60_000L;
        account.settleBuyTrade(50_000L, 60_000L, 0L);

        assertThat(account.getLockedBalance()).isEqualTo(0L);
        assertThat(account.getAvailableBalance()).isEqualTo(availableBeforeLock - 50_000L);
    }

    // settleBuyTrade — 수수료 포함
    @Test
    void settleBuyTrade_수수료_포함_차액_환급() {
        // 주문가 12000, 체결가 10000, 수량 5
        // lockedAmount = 60000 + 6(수수료lock) = 60006
        // tradeAmount = 50000, feeAmount = 5
        // 환급 = 60006 - 50000 - 5 = 10001
        account.lockBalance(60_006L);
        account.settleBuyTrade(50_000L, 60_006L, 5L);

        assertThat(account.getLockedBalance()).isEqualTo(0L);
        assertThat(account.getAvailableBalance()).isEqualTo(-60_006L + (60_006L - 50_000L - 5L));
    }

    // settleSellTrade — 수수료 없음 (기존 로직 검증)
    @Test
    void settleSellTrade_매도_대금_수령() {
        account.settleSellTrade(50_000L, 0L);

        assertThat(account.getAvailableBalance()).isEqualTo(50_000L);
    }

    // settleSellTrade — 수수료 포함
    @Test
    void settleSellTrade_수수료_차감_후_수령() {
        // tradeAmount = 50000, feeAmount = 5
        // 수령액 = 50000 - 5 = 49995
        account.settleSellTrade(50_000L, 5L);

        assertThat(account.getAvailableBalance()).isEqualTo(49_995L);
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
        account.lockBalance(60_000L);
        account.relockBalance(60_000L, 100_000L);

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

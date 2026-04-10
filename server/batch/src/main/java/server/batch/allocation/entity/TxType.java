package server.batch.allocation.entity;

public enum TxType {
    DEPOSIT, WITHDRAWAL, ORDER_LOCK, ORDER_UNLOCK, TRADE_SETTLEMENT, DIVIDEND_DEPOSIT
    // 입금 출금 주문잠금 주문해제 체결정산 배당입금
}

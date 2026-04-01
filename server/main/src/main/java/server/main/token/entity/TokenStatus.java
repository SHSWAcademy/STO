package server.main.token.entity;

public enum TokenStatus {
    ISSUED,     // 발행완료
    TRADING,    // 거래 중
    SUSPENDED,  // 거래 중단
    CLOSED      // 거래 완료
}

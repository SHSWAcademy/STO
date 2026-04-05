package server.main.token.entity;

public enum TokenStatus {
    ISSUED,     // 발행완료
    TRADING,    // 거래 중
    SUSPENDED,  // 거래 중단 (일시정지)
    CLOSED      // 거래 완료 (상장페지)
}

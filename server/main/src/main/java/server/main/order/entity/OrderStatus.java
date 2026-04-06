package server.main.order.entity;

public enum OrderStatus {
    OPEN,       // 접수
    PENDING,    // 대기 -> 매치쪽에서 필요한지 ?
    PARTIAL,    // 부분 체결
    FILLED,     // 전체 체결
    CANCELLED,  // 취소
    FAILED      // 실패
}

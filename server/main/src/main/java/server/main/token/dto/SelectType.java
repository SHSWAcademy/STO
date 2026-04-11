package server.main.token.dto;

// 메인 화면에서 받아올 데이터 종류 (기본, 거래 대금, 거래량)
public enum SelectType {
    BASIC,
    TOTAL_TRADE_VALUE,
    TOTAL_TRADE_QUANTITY
}

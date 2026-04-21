package server.main.global.error;

import org.springframework.http.HttpStatus;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 도연님 에러코드 확인해보시고 유동적으로 추가나 삭제 부탁드립니다 ! 아래는 예시라서

    // ── 시스템 ──────────────────────────────────────────────
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "INVALID_INPUT_VALUE", "입력값이 올바르지 않습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다."),
    ENTITY_NOT_FOUNT_ERROR(HttpStatus.NOT_FOUND, "CANNOT_FIND_ENTITY", "잘못된 접근입니다."),

    // ── 인증/인가 ────────────────────────────────────────────
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "유효하지 않은 토큰입니다."),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "만료된 토큰입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "아이디 또는 비밀번호가 올바르지 않습니다."),

    // ── 회원 ──────────────────────────────────────────────
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "회원을 찾을 수 없습니다."),
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 사용 중인 이메일입니다."),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "INVALID_PASSWORD", "비밀번호가 올바르지 않습니다."),
    WRONG_CURRENT_PASSWORD(HttpStatus.BAD_REQUEST, "WRONG_CURRENT_PASSWORD", "현재 비밀번호가 올바르지 않습니다."),
    INSUFFICIENT_BALANCE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_BALANCE", "원화 잔고가 부족합니다."),
    INSUFFICIENT_TOKEN_BALANCE(HttpStatus.BAD_REQUEST, "INSUFFICIENT_TOKEN_BALANCE", "보유 토큰이 부족합니다."),
    INVALID_AMOUNT(HttpStatus.BAD_REQUEST, "INVALID_AMOUNT", "금액은 0보다 커야 합니다."),
    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "ACCOUNT_NOT_FOUND", "계좌를 찾을 수 없습니다."),

    // ── 토큰(부동산) ─────────────────────────────────────────
    TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "TOKEN_NOT_FOUND", "토큰을 찾을 수 없습니다."),
    TOKEN_NOT_TRADABLE(HttpStatus.BAD_REQUEST, "TOKEN_NOT_TRADABLE", "거래 가능한 상태가 아닙니다."),
    TOKEN_ALREADY_LISTED(HttpStatus.CONFLICT, "TOKEN_ALREADY_LISTED", "이미 거래 개시된 토큰입니다."),

    // ── 주문 ──────────────────────────────────────────────
    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND, "ORDER_NOT_FOUND", "주문을 찾을 수 없습니다."),
    ORDER_CANNOT_CANCEL(HttpStatus.BAD_REQUEST, "ORDER_CANNOT_CANCEL", "취소할 수 없는 주문 상태입니다."),
    ORDER_NOT_OWNED(HttpStatus.FORBIDDEN, "ORDER_NOT_OWNED", "본인의 주문이 아닙니다."),
    ORDER_NOT_MODIFIABLE(HttpStatus.BAD_REQUEST, "ORDER_NOT_MODIFIABLE", "수정할 수 없는 주문 상태입니다."),
    INVALID_UPDATE_QUANTITY(HttpStatus.BAD_REQUEST, "INVALID_UPDATE_QUANTITY", "이미 체결된 수량보다 적은 수량으로 수정할 수 없습니다."),
    INVALID_TICK_SIZE(HttpStatus.BAD_REQUEST, "INVALID_TICK_SIZE", "호가 단위에 맞지 않는 가격입니다."),

    PRICE_OVER_LIMIT(HttpStatus.BAD_REQUEST, "PRICE_OVER_LIMIT", "상한가를 초과한 주문 가격입니다."),
    PRICE_UNDER_LIMIT(HttpStatus.BAD_REQUEST, "PRICE_UNDER_LIMIT", "하한가 미만의 주문 가격입니다."),

    // ── 거래 시간 ────────────────────────────────────────────
    OUTSIDE_TRADING_HOURS(HttpStatus.BAD_REQUEST, "OUTSIDE_TRADING_HOURS", "거래 시간이 아닙니다. (09:00 ~ 15:30)"),

    // ── Match 서비스 ──────────────────────────────────────────
    MATCH_SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "MATCH_SERVICE_UNAVAILABLE",
            "체결 서비스 연결 실패로 주문이 처리되지 않았습니다."),

    // ── 관심 종목 ─────────────────────────────────────────────
    WATCHLIST_ALREADY_EXISTS(HttpStatus.CONFLICT, "WATCHLIST_ALREADY_EXISTS", "이미 관심 종목에 추가된 토큰입니다."),
    WATCHLIST_NOT_FOUND(HttpStatus.NOT_FOUND, "WATCHLIST_NOT_FOUND", "관심 종목에 없는 토큰입니다."),

    // ── 배당 ──────────────────────────────────────────────────
    DIVIDEND_NO_HOLDERS(HttpStatus.BAD_REQUEST, "DIVIDEND_NO_HOLDERS", "해당 토큰의 보유자가 없습니다."),
    TOKEN_NOT_TRADING(HttpStatus.BAD_REQUEST, "TOKEN_NOT_TRADING", "거래 중인 토큰에만 배당을 지급할 수 있습니다."),
    ALLOCATION_ALREADY_EXISTS(HttpStatus.CONFLICT, "ALLOCATION_ALREADY_EXISTS", "이미 해당월에 배당이 등록되어 있습니다."),
    ALLOCATION_UPDATE_NOT_ALLOWED(HttpStatus.CONFLICT, "ALLOCATION_UPDATE_NOT_ALLOWED", "이미 지급된 배당은 수정이 불가합니다."),

    // ── 지갑 ──────────────────────────────────────────────────
    ISSUER_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "ISSUER_WALLET_NOT_FOUND", "ISSUER 지갑을 찾을 수 없습니다."),
    TREASURY_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "TREASURY_WALLET_NOT_FOUND", "TREASURY 지갑을 찾을 수 없습니다."),
    SELLER_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "SELLER_WALLET_NOT_FOUND", "SELLER 지갑을 찾을 수 없습니다."),
    BUYER_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "BUYER_WALLET_NOT_FOUND", "BUYER 지갑을 찾을 수 없습니다."),
    WALLET_CREATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WALLET_CREATION_FAILED", "지갑 생성에 실패했습니다."),
    WALLET_ALREADY_EXISTS(HttpStatus.CONFLICT, "WALLET_ALREADY_EXISTS", "이미 지갑이 존재합니다."),
    WALLET_ENCRYPTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "WALLET_ENCRYPTION_FAILED", "지갑 키 암호화에 실패했습니다."),
    MEMBER_WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_WALLET_NOT_FOUND", "MEMBER 지갑을 찾을 수 없습니다."),

    // ── 블록체인 ──────────────────────────────────────────────────
    CONTRACT_DEPLOY_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CONTRACT_DEPLOY_FAILED", "컨트랙트 배포에 실패했습니다."),
    ONCHAIN_TRANSACTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "ONCHAIN_TRANSACTION_FAILED", "온체인 거래 기록에 실패했습니다.");


    // ── 파일 ──────────────────────────────────────────────────
    private final HttpStatus httpStatus;
    private final String errorCode;
    private final String message;
}

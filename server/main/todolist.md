# /trading 페이지 - 차트 / 호가 / 시세 구현 TODO

## 전체 아키텍처 개요



---

## 호가창 실시간 방식: 방식 B 채택 (초기 Snapshot + 이후 Pub/Sub)

### 방식 A — 변경마다 Pub/Sub만 사용
- **장점**: 구조 단순
- **단점**: 최초 연결 시 빈 화면

### 방식 B — 초기 Snapshot + 이후 Pub/Sub (채택)
- **장점**: 연결 즉시 현재 호가 표시. 실거래소 표준 패턴.
- **단점**: snapshot REST + pub/sub 두 경로 관리 필요

---

## ✅ 완료된 작업

### [0] 사전 정리
- [x] `TokenController` 주석 정리
- [x] `TokenServiceImpl`에서 미사용 `CandleService` 의존성 제거
- [x] `MainApplication`에서 `@EnableJpaAuditing` 중복 제거 (`JpaConfig`에만 유지)
- [x] `BaseEntity`, `Token` — `@SuperBuilder(toBuilder = true)` 수정

### [1] build.gradle 의존성 추가
- [x] `spring-boot-starter-websocket` 추가

### [2] WebSocket 설정
- [x] `WebSocketConfig.java` 생성 (STOMP, SockJS, `/ws/trading` 엔드포인트)
- [x] `SecurityConfig`에 `/ws/trading/**` permitAll 추가

### [3] Redis Pub/Sub 구독 설정
- [x] `RedisConfig`에 `RedisMessageListenerContainer` 빈 추가
- [x] `RedisConfig`에 `MessageListenerAdapter` 빈 추가
- [x] `global/websocket/RedisSubscriber.java` 생성
  - `orderBook:{tokenId}` 수신 → `/topic/orderBook/{tokenId}` 브로드캐스트
  - `trades:{tokenId}` 수신 → `/topic/trades/{tokenId}` 브로드캐스트

### [4] 엔티티 설계
- [x] `order/entity/Order.java` 생성 (BaseEntity 상속, OrderType/OrderStatus enum 포함)
- [x] `trade/entity/Trade.java` 생성 (ManyToOne seller/buyer, SettlementStatus enum 포함)
- [x] `order/`, `trade/` 패키지 구조 생성 (controller/service/repository/mapper)

### [5] Snapshot 핸들러 (방식 B 핵심)
- [x] `global/websocket/MatchClient.java` 생성
  - `getOrderBookSnapshot(tokenId)` — match REST 호출
  - `sendOrder(MatchOrderRequestDto)` — 주문 match 서버 전달
- [x] `global/websocket/OrderBookSubscribeHandler.java` 생성
  - `@EventListener(SessionSubscribeEvent)` 구독 감지
  - `/topic/orderBook/{tokenId}` 구독 시 snapshot 즉시 전송
- [x] `JpaConfig`에 `RestTemplate` 빈 등록
- [x] `application.properties`에 `match.server.url` 추가

### [8] 주문 접수
- [x] `OrderRequestDto.java` 생성 (orderType, orderPrice, orderQuantity, @NotNull)
- [x] `MatchOrderRequestDto.java` 생성 (match 서버 전달용)
- [x] `OrderController` — `POST /api/token/{tokenId}/order` 구현
- [x] `OrderServiceImpl.createOrder()` — JWT 사용자 추출, 잔고 검증, DB 저장, match 전달

---

## ⬜ 남은 작업

### [6] 캔들 차트 API (main, REST)

- [ ] `CandleController.java` 생성
  - `GET /api/token/{tokenId}/candles?type={minute|hour|day|month|year}`
- [ ] `CandleService`에 타입별 완성된 캔들 리스트 조회 메서드 추가
  - 현재 진행 중인 봉 제외, 완성된 봉만 반환
- [ ] `CandleResponseDto.java` 생성 (openPrice, highPrice, lowPrice, closePrice, volume, candleTime, tradeCount)
- [ ] 각 CandleRepository에 시간 범위 조회 쿼리 추가

#### [6-추가] 현재 진행 중인 봉 실시간 반영 (batch 서버 작업 시 함께)
- **기술**: Redis Pub/Sub 기존 파이프라인 재사용
  - channel: `candle:{type}:{tokenId}`
  - `/topic/candle/{type}/{tokenId}` WebSocket 브로드캐스트

### [7] match 서버 작업 (팀원)

- [ ] `build.gradle`에 Redis 의존성 추가
- [ ] 호가 데이터 구조 설계 (TreeMap 기반)
- [ ] 주문 접수 API `POST /internal/orders`
- [ ] 체결 엔진 구현
- [ ] Redis Publisher — 호가 변경 시 `orderBook:{tokenId}` publish
- [ ] Redis Publisher — 체결 완료 시 `trades:{tokenId}` publish
- [ ] Snapshot API `GET /internal/orderBook/{tokenId}`

#### [7-중요] TokenHolding 생성 시점 — 팀원 공유 필요

> TokenHolding은 **첫 BUY 주문 접수 시가 아니라, 첫 BUY 체결 완료 시** match 서버가 생성해야 함

**이유**:
- 주문만 넣고 취소하거나 미체결 상태면 실제 보유가 아님
- 체결 완료 시점에 비로소 토큰을 실제로 보유하게 됨

**match 서버 체결 완료 후 처리 순서**:
```
체결 완료
    → TRADES 테이블 저장
    → buyer의 TokenHolding 없으면 생성 (currentQuantity = 체결 수량)
    → buyer의 TokenHolding 있으면 currentQuantity += 체결 수량
    → seller의 TokenHolding currentQuantity -= 체결 수량
    → Redis publish trades:{tokenId}
```

**main 서버 SELL 검증 코드 영향**:
- TokenHolding 없으면 `EntityNotFoundException` 대신 `INSUFFICIENT_TOKEN_BALANCE` 반환이 더 적절
- TokenHolding 없음 = 보유 토큰 없음 = 판매 불가로 처리

### [8] 주문 관련

- [x] `GET /api/token/{tokenId}/orders/pending` — 미체결 주문 조회 구현
- [x] `DELETE /api/orders/{orderId}` — 주문 취소 구현
- [x] `OrderServiceImpl.getPendingOrders()` — JWT 사용자 추출, OPEN/PENDING/PARTIAL 상태 필터링
- [x] `OrderServiceImpl.cancelOrder()` — 본인 주문 검증 후 CANCELLED 처리
- [x] `OrderRepository`에 미체결 주문 조회 쿼리 추가
  - `findByToken_TokenIdAndMember_MemberIdAndOrderStatusIn(...)`
- [x] `PendingOrderResponseDto.java` 생성
- [x] `OrderServiceImpl` SELL 검증 — tokenHolding 없을 때 `EntityNotFoundException` → `INSUFFICIENT_TOKEN_BALANCE`로 수정

#### [8-TODO] match 서버 연동 후 추가 필요
- [ ] `cancelOrder()`에 match 서버 취소 요청 전달 (match 서버 cancel API 구현 후)

### [9] 프론트엔드 연동 (match 서버 완성 후)

- [ ] WebSocket 연결 코드 작성 (SockJS + STOMP)
- [ ] 호가창 목업 → `/topic/orderBook/{tokenId}` WebSocket 데이터로 교체
- [ ] 시세창 목업 → `/topic/trades/{tokenId}` WebSocket 데이터로 교체
- [ ] 주문 버튼 → `POST /api/token/{tokenId}/order` 호출로 교체
- [ ] 토큰 정보 → `GET /api/token/{tokenId}` 호출로 교체
- [ ] 미체결 주문 → `GET /api/token/{tokenId}/orders/pending` 호출로 교체
- [ ] 캔들 차트 → `GET /api/token/{tokenId}/candles` 호출로 교체

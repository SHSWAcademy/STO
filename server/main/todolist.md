# /trading 페이지 - 차트 / 호가 / 시세 구현 TODOList



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

### [10] 시세·체결 목록 API (main, REST + WebSocket) ✅ 완료 (2026-04-04)

- [x] `TradeResponseDto.java` 생성 (tradePrice, tradeQuantity, percentageChange, totalVolume, executedAt)
- [x] `TradeRepository` — `findTradeList(tokenId)` @Query (최근 50건), `sumDailyVolume(tokenId)` @Query (당일 누적 합산)
- [x] `TradeMapper.java` — `toDto(Trade)` MapStruct 매핑 (percentageChange ignore)
- [x] `TradeServiceImpl.getTrades()` — 50건 조회 + 당일 totalVolume 계산 후 각 dto에 세팅
- [x] `TradeController` — `GET /api/token/{tokenId}/trades`
- [x] `RedisSubscriber` — `trades:{tokenId}` → `/topic/trades/{tokenId}` 브로드캐스트 (기존 구현)

#### [10-이슈] totalVolume 런닝 토탈 미구현
> **현재**: 당일 총합 단일값을 모든 row에 동일하게 세팅 → 모든 체결 row가 같은 totalVolume 값을 가짐
>
> **원래 비즈니스 요구**: 각 체결 row에 그 시점까지의 누적 거래량이 표시되어야 함 (런닝 토탈)
>
> **판단**: 지금은 당일 총합으로 퉁치고, 프론트에서 테이블 상단에 "총 거래량: N주" 하나로 표시. 런닝 토탈은 추후 개선.
>
> **추후 개선 방법**: match 서버가 체결 시 누적값을 함께 저장하거나, 각 체결 시점 이전 SUM을 윈도우 함수로 계산

---

### [6] 캔들 차트 API (main, REST + WebSocket) ✅ 완료

- [x] `Candle.java` — `@MappedSuperclass`로 전환
- [x] `CandleMinute`, `CandleHour`, `CandleDay`, `CandleMonth`, `CandleYear` — `extends Candle` 완료
- [x] `CandleResponseDto.java` 생성
- [x] `CandleMapper.java` 생성 (MapStruct)
- [x] 5개 Repository — `findTop35Before(tokenId, before)` 쿼리 추가
- [x] `CandleServiceImpl.getCandles()` — `CandleType` enum 기반 타입별 분기, truncatedTo 기준 시각 계산
- [x] `CandleController` — `GET /api/token/{tokenId}/candles?type={MINUTE|HOUR|DAY|MONTH|YEAR}`
- [x] `RedisSubscriber` — `candle:{type}:{tokenId}` → `/topic/candle/{type}/{tokenId}` 브로드캐스트 추가

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

- [x] `POST /api/token/{tokenId}/order` — 매수/매도 주문 접수 구현
- [x] `OrderServiceImpl.createOrder()` — JWT 사용자 추출, 잔고 검증, DB 저장, match 전달
- [x] `PendingOrderResponseDto.java` 생성 — 필드: orderId, orderType, orderStatus, orderPrice, orderQuantity, filledQuantity, remainingQuantity, createdAt, updatedAt
- [x] `OrderServiceImpl` SELL 검증 — tokenHolding 없을 때 `EntityNotFoundException` → `INSUFFICIENT_TOKEN_BALANCE`로 수정
- [x] `OrderServiceImpl.createOrder()` — `orderStatus(OrderStatus.OPEN)` 세팅 추가
- [x] `OrderMapper` — `toPendingDto()`, `toPendingDtoList()` 추가
- [x] `OrderRepository` — `findPendingOrderByMemberAndToken()` 쿼리 추가 (member_id, token_id, orderStatus IN OPEN/PENDING/PARTIAL)
- [x] `OrderServiceImpl.getPendingOrders()` 구현 — JWT memberId 추출 → DB 조회 → DTO 변환
- [x] `OrderController` — `GET /api/token/{tokenId}/order/pending` 엔드포인트 추가
- [x] `TokenController` — `GET /api/token/{tokenId}` 엔드포인트 추가 (`@RequestMapping("/api/token")` 포함)

#### [8-TODO] match 서버 완성 → 구현 순서 ✅ 완료 (2026-04-06)

**1단계 — `cancelOrder()` 구현**
- [x] `Order` 엔티티에 `removeOrder()` 소프트 딜리트 메서드 추가
- [x] `OrderRepository`에 본인 주문 조회 쿼리 추가 (`findByMemberIdAndOrderId` + `@Param`)
- [x] `OrderServiceImpl.cancelOrder()` 구현
  - JWT memberId 추출 → 본인 주문 검증 → 상태 검증(OPEN/PENDING/PARTIAL) → 잔고/수량 복구 → soft delete → match 전달
- [x] `Account.cancelOrder(amount)` — locked → available 복구
- [x] `MemberTokenHolding.cancelOrder(qty)` — locked → current 복구
- [x] `MatchClient.cancelOrder(orderId)` — `restTemplate.delete(url)`
- [x] `OrderController` — `DELETE /api/token/order/cancel/{orderId}` 엔드포인트

**2단계 — `updateOrder()` 구현**
- [x] `UpdateOrderRequestDto` — `updatePrice`, `updateQuantity` 필드 추가
- [x] `UpdateMatchOrderRequestDto` — `orderId`, `orderSequence`, `updatePrice`, `updateQuantity`
- [x] `Order.updateOrder(price, qty)` — 엔티티 수정 메서드
- [x] `Account.relockBalance(oldAmount, newAmount)` — 잔고 재조정
- [x] `MemberTokenHolding.relockQuantity(oldQty, newQty)` — 수량 재조정
- [x] `OrderServiceImpl.updateOrder()` — 상태 검증 → 잔고/수량 재조정 → DB 수정 → match 전달
- [x] `MatchClient.updateOrder(dto)` — `restTemplate.put(url, dto)`
- [x] `OrderController` — `PUT /api/token/order/update/{orderId}` 엔드포인트

**기타 수정**
- [x] `createOrder()` — `remainingQuantity(dto.getOrderQuantity())` 세팅 추가
- [x] `createOrder()` — BUY 시 `lockBalance()`, SELL 시 `lockQuantity()` 호출 추가
- [x] `ErrorCode.ORDER_NOT_MODIFIABLE` HTTP 상태 `304` → `400` 수정

**3단계 — 대기탭 WebSocket (`getPendingOrders`)** ← match 서버 완성 후
- [ ] match 서버와 채널명 합의 (`pendingOrders:{tokenId}:{memberId}` 권장)
- [ ] `RedisSubscriber`에 `pendingOrders` 케이스 추가
- [ ] `PendingOrderSubscribeHandler` 생성 — 구독 시 JWT로 memberId 추출 후 DB snapshot 즉시 전송

> **주의**: 구독 주소 `/topic/pendingOrders/{tokenId}/{memberId}` — WebSocket 헤더에서 JWT 파싱 필요
> match 서버가 체결/취소/수정 시 `pendingOrders:{tokenId}:{memberId}` publish 해야 실시간 갱신됨

### [9] 프론트엔드 연동 ← 지금 작업 시작, match 서버가 이 작업 기준으로 맞춰줌

> **작업 방식**: 프론트에서 먼저 WebSocket 데이터 구조(형식)를 정의하면, match/batch 서버가 그 형식에 맞게 publish

#### [9-공통] WebSocket 연결 코드 작성
- [ ] SockJS + STOMP 클라이언트 설정 (공통 훅 또는 유틸로 분리)
  - `/ws/trading` 연결
  - tokenId 기반 topic 구독/해제 (페이지 이동 시 구독 해제 필수)

#### [9-1] 호가창 — `HogaPanel.jsx`
- [ ] `HOGA_ASKS`, `HOGA_BIDS` mock → WebSocket `/topic/orderBook/{tokenId}` 실시간 데이터로 교체
- [ ] match 서버에 전달할 데이터 형식 정의 (매도호가 배열, 매수호가 배열)
  ```json
  {
    "asks": [{ "price": 12200, "amount": 300 }, ...],
    "bids": [{ "price": 12100, "amount": 500 }, ...]
  }
  ```

#### [9-2] 시세·체결 목록 — `ChartPanel.jsx` 하단
- [ ] `PRICE_HISTORY_ROWS`, `HOGA_EXECUTIONS` mock → WebSocket `/topic/trades/{tokenId}` 실시간 데이터로 교체
- [ ] match 서버에 전달할 데이터 형식 정의 (체결 단건)
  ```json
  {
    "price": 12150,
    "quantity": 10,
    "tradeTime": "10:29:56",
    "isBuy": true
  }
  ```

#### [9-3] 캔들 차트 — `ChartPanel.jsx` 상단
- [ ] `CHART_DATA` mock → `GET /api/token/{tokenId}/candles?type=MINUTE` HTTP 호출로 교체
- [ ] 차트 기간 버튼(`1분`, `일`, `주`, `월`, `년`) 클릭 시 type 파라미터 변경 후 재조회
- [ ] WebSocket `/topic/candle/{type}/{tokenId}` 구독 → 새 봉 수신 시 차트 끝에 append

#### [9-4] 주문창 — `OrderPanel.jsx`
- [ ] 매수/매도 버튼 → `POST /api/token/{tokenId}/order` 호출로 교체
- [ ] 대기탭 미체결 목록 → WebSocket 연동 (match 서버 완성 후)
- [ ] 취소/수정 버튼 → `DELETE`, `PATCH /api/orders/{orderId}` 호출로 교체 (match 서버 완성 후)


#### [match 서버 팀원 공유] 실시간 체결 WebSocket 데이터 형식
> match 서버가 `trades:{tokenId}` publish 시 아래 필드 포함 필요
> - `percentageChange` — (현재 체결가 - 이전 체결가) / 이전 체결가 * 100 (match에서 계산)
> - `totalVolume` — 당일 누적 체결 수량 (match에서 계산)
> main 서버는 그대로 relay만 함
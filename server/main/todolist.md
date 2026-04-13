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

### [6] 캔들 차트 API (main, REST + WebSocket)

#### [6-1] 과거 봉 REST API ✅ 완료
- [x] `Candle.java` — `@MappedSuperclass`로 전환
- [x] `CandleMinute`, `CandleHour`, `CandleDay`, `CandleMonth`, `CandleYear` — `extends Candle` 완료
- [x] `CandleResponseDto.java` 생성 (openPrice, highPrice, lowPrice, closePrice, volume, candleTime, tradeCount)
- [x] `CandleMapper.java` 생성 (MapStruct)
- [x] 5개 Repository — `findTop35Before(tokenId, before)` 쿼리 추가
- [x] `CandleServiceImpl.getCandles()` — `CandleType` enum 기반 타입별 분기, truncatedTo 기준 시각 계산
- [x] `CandleController` — `GET /api/token/{tokenId}/candle?type={MINUTE|HOUR|DAY|MONTH|YEAR}`
  > 클라이언트가 상세 페이지 진입 시 이 API로 과거 봉 35개를 가져와 차트에 렌더링
  >
  > **주의**: Batch 방식 제거 예정 ([6-3] 참고). CandleLiveManager가 구간 경계에서 직접 DB 저장하는 방식으로 전환. 기존 Batch Processor의 NULL 버그는 더 이상 수정 불필요.

#### [6-2] Batch Redis candle publish 제거 ✅ 완료 (2026-04-07)
- [x] `RedisSubscriber` — `candle` 타입 케이스 삭제
- [x] Batch Writer 5개 (Minute/Hour/Day/Month/Year) — Redis publish 코드 전부 제거

#### [6-3] 캔들 전면 재설계 — Batch 제거, Main 단독 관리 ✅ 완료 (2026-04-08)

> **설계 확정 (2026-04-08)**
>
> 기존 Batch 방식을 제거하고 Main 서버가 캔들을 전담한다.
>
> - **Batch**: `CandleScheduler` `@Scheduled` 전부 주석 처리 → 캔들 배치 비활성화
> - **CandleLiveManager** (메모리): 체결 이벤트 수신 시 5개 주기(분/시/일/월/년) 봉을 동시에 갱신
> - **구간 경계 도달 시**: 확정 봉을 CandleLiveManager가 직접 DB에 저장 → 다음 봉으로 초기화
> - **REST**: DB에 저장된 확정 봉 제공 (기존 동일)
> - **WS**: 사용자 구독 시 현재 봉 스냅샷 즉시 전송 + 이후 체결마다 갱신 push
> - **스냅샷 전송 방식**: 상세 페이지 진입 시 분/시/일/월/년 5개 주기 스냅샷을 한꺼번에 전송
>   → 사용자가 버튼 클릭으로 주기 전환 시 이미 데이터가 있어 즉시 표시 가능
>
> **핵심**: 체결 1건 → 5개 주기 봉 동시 갱신 (실서비스 표준 방식)

**구현 순서:**

**[Step 1] Batch 비활성화**
- [ ] `CandleScheduler.java` — `@Scheduled` 5개 전부 주석 처리

**[Step 2] LiveCandle 상태 클래스 생성**
- [ ] `candle/LiveCandle.java` 생성
  - 필드: `openPrice`, `highPrice`, `lowPrice`, `closePrice`, `volume`, `tradeCount`, `candleStartTime`

**[Step 3] CandleLiveManager 생성**
- [ ] `candle/CandleLiveManager.java` 생성 — `@Component`
  - 5개 `ConcurrentHashMap<Long, LiveCandle>` (분/시/일/월/년별)
  - `update(tokenId, tradePrice, tradeQuantity)`: 체결 수신 시 5개 주기 동시 갱신
    - 각 주기별 `candleStartTime` 계산 (분: truncatedTo MINUTES, 시: HOURS, 일: DAYS, 월: 1일, 년: 1월1일)
    - 구간이 바뀌었으면 → 이전 봉 DB 저장 → 새 봉 초기화
    - 같은 구간이면 → high/low/close/volume/tradeCount 갱신
  - `getSnapshot(tokenId, candleType)`: 구독 시 현재 봉 상태 반환
  - 각 주기별 Repository 주입 필요 (CandleMinuteRepository ~ CandleYearRepository)

**[Step 4] RedisSubscriber `trades` 케이스 확장**
- [ ] body에서 `tradePrice`, `tradeQuantity` 추출
- [ ] `CandleLiveManager.update(tokenId, tradePrice, tradeQuantity)` 호출
- [ ] 갱신된 현재 봉을 `/topic/candle/live/{tokenId}` 로 브로드캐스트
  - payload에 `candleType` 포함 (클라이언트가 어느 주기 봉인지 구분)

**[Step 5] CandleLiveSubscribeHandler 생성**
- [ ] `candle/CandleLiveSubscribeHandler.java` 생성
  - `@EventListener(SessionSubscribeEvent)` — `/topic/candle/live/{tokenId}` 구독 감지
  - `CandleLiveManager.getSnapshot(tokenId, candleType)` 조회
  - 해당 세션에만 현재 봉 스냅샷 즉시 전송

**[Step 6] match 서버 팀원 공유**
- [ ] `trades:{tokenId}` payload에 `tradePrice`, `tradeQuantity` 필드 반드시 포함 필요

#### [6-3 설계 노트] CandleFlushService 별도 Bean 분리 이유
> **배경**: `CandleLiveManager`의 `saveToDB()`에 `@Transactional`을 붙여도 동작하지 않아 `CandleFlushService`로 분리함.
>
> **이유**: Spring의 `@Transactional`은 AOP 프록시 기반으로 동작한다. 같은 클래스 내부에서 `this.saveToDB()`를 호출하면 프록시를 거치지 않고 실제 객체를 직접 호출하기 때문에 `@Transactional`이 무시된다. 클래스 레벨이든 메서드 레벨이든 동일하다.
>
> **해결**: `CandleFlushService`를 별도 Bean으로 분리하면 Spring이 주입 시 프록시 객체를 주입하므로, `candleFlushService.saveToDB()` 호출이 프록시를 거쳐 `@Transactional`이 정상 적용된다.
>
> **트랜잭션 전파**: `CandleLiveManager`에는 `@Transactional`이 없으므로 `saveToDB()` 호출 시점에 활성 트랜잭션이 없다. 따라서 기본값 `REQUIRED`로 충분하며, 호출마다 독립적인 트랜잭션이 생성된다 → `flushExpiredCandles()`의 forEach 루프에서 tokenId별로 독립 커밋/롤백 보장.

**현재 봉 WebSocket 응답 예시 (main → 클라이언트):**
```json
{
  "candleType": "MINUTE",
  "openPrice": 12100,
  "highPrice": 12200,
  "lowPrice": 12050,
  "closePrice": 12150,
  "volume": 340,
  "tradeCount": 5,
  "candleTime": "2026-04-08T09:27:00"
}
```

#### [6-4] 프론트엔드 캔들 차트 연동 ⬜ 미구현 (내일 작업, [9-3]과 동일)

> **[설계 이슈] CandleLiveSubscribeHandler 구독 시 모든 타입 스냅샷 일괄 전송**
>
> 현재 `/topic/candle/live/{tokenId}` 구독 시 MINUTE/HOUR/DAY/MONTH/YEAR 5개 타입 스냅샷을 한꺼번에 전송함.
> 클라이언트가 1분봉만 보고 있어도 나머지 4개를 함께 수신하게 됨. `CandleResponseDto`의 `candleType` 필드로 구분은 가능하므로 기능 상 문제는 없음.
> 추후 트래픽이 증가하면 타입별 토픽(`/topic/candle/live/{tokenId}/{type}`)으로 분리하는 것을 고려.
- [ ] 상세 페이지 진입 시 `GET /api/token/{tokenId}/candle?type=MINUTE` 호출 → 과거 봉 35개 렌더링
- [ ] 차트 기간 버튼(`1분`, `시간`, `일`, `달`, `년`) 클릭 시 type 파라미터 변경 후 재조회
- [ ] WebSocket `/topic/candle/live/{tokenId}` 구독 → 수신 시 현재(마지막) 봉의 고가/저가 실시간 업데이트

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

**3단계 — 대기탭 WebSocket (`getPendingOrders`)** ✅ 완료 (2026-04-06)
- [x] match 서버와 채널명 합의 (`pendingOrders:{tokenId}:{memberId}`)
- [x] `RedisSubscriber`에 `pendingOrders` 케이스 추가
- [x] `PendingOrderSubscribeHandler` 생성 — 구독 시 JWT 검증 + memberId 본인 확인 + DB snapshot 즉시 전송
- [x] `OrderBookSubscribeHandler` — JWT 로그인 여부 검증 추가
- [x] WebSocket 헤더 `Authorization: Bearer {token}` 검증 적용 (전체 핸들러)

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

#### [9-3] 캔들 차트 — `MockupPage.jsx` (`/mockup/:tokenId`) ✅ 완료 (2026-04-07)

- [x] `CHART_DATA` mock → `GET /api/token/{tokenId}/candle?type={MINUTE|HOUR|DAY|MONTH|YEAR}` REST 호출로 교체
  - URL 버그 수정: `/candles` → `/candle` (단수)
- [x] 차트 기간 버튼 레이블 수정: `1분·일·주·월·년` → `분·시간·일·월·년` (CandleType enum 순서와 일치)
  - 기본값: `분` (MINUTE)
- [x] 기간 버튼 클릭 시 `type` 파라미터 변경 후 REST 재조회
- [x] WebSocket `/topic/candle/live/{tokenId}` 구독 → 수신 시 현재(마지막) 봉 고가/저가 실시간 업데이트
  - `useTradingSocket`에 `candleType='live'` 전달 → `/topic/candle/live/{tokenId}` 구독
  - 현재 봉 시간(`time`)이 동일하면 마지막 봉 교체, 새 시간이면 봉 추가
  - ※ 백엔드 [6-3] (`CandleLiveManager`) 구현 완료 후 실시간 동작

> **현재 봉 WS 수신 payload 형식 (백엔드 [6-3] 구현 시 맞춰야 할 형식)**
> ```json
> { "highPrice": 12200, "lowPrice": 12050, "openPrice": 12100, "candleTime": "2026-04-07T10:29:00" }
> ```

#### [9-4] 주문창 — `MockupPage.jsx` (`LoginGateOrderPanel`) ✅ 완료 (2026-04-07)
- [x] 매수/매도 버튼 → `POST /api/token/{tokenId}/order` 실제 API 호출로 교체
  - 성공/실패 메시지 인라인 표시
  - 비로그인 시 로그인 안내 팝업
- [x] 대기탭 → `GET /api/token/{tokenId}/order/pending` 초기 스냅샷 조회
- [x] 취소 버튼 → `DELETE /api/token/order/cancel/{orderId}` 호출
- [ ] 대기탭 실시간 갱신 → WebSocket `pendingOrders:{tokenId}:{memberId}` (match 서버 완성 후)
- [ ] 수정 버튼 → `PUT /api/token/order/update/{orderId}` (match 서버 완성 후)
- [ ] **PENDING 상태 UX 처리** — 백엔드에서 PENDING(match 처리 중) 상태 주문은 수정/취소 불가 (의도적 설계). 대기탭에서 orderStatus가 PENDING인 주문의 취소/수정 버튼 비활성화 처리 필요.

#### [9-5] 기타 상세 페이지 연동 ✅ 완료 (2026-04-07)
- [x] 종목정보 탭 → `GET /api/token/{tokenId}/info` 호출 (발행가·총자산·주소·상장일)
- [x] 배당금 탭 → `GET /api/token/{tokenId}/allocation` 호출 (테이블 렌더링)
- [x] 공시 탭 → `GET /api/token/{tokenId}/disclosure` 호출 (아코디언 렌더링)
- [x] 시세 섹션 '일별' 탭 제거 — 실시간 단일 표시로 변경
- [x] 상단 헤더 1일최고/최저·52주 통계 제거 (`hideStats` prop)
- [x] 주문창 항상 렌더링 (백엔드 미응답 시에도 매수/매도 탭 표시)


#### [match 서버 팀원 공유] 실시간 체결 WebSocket 데이터 형식
> match 서버가 `trades:{tokenId}` publish 시 아래 필드 포함 필요
> - `percentageChange` — (현재 체결가 - 이전 체결가) / 이전 체결가 * 100 (match에서 계산)
> - `totalVolume` — 당일 누적 체결 수량 (match에서 계산)
> main 서버는 그대로 relay만 함
> 


#### 캔들차트 구현 
// 변경 전
stompClient.subscribe(`/topic/candle/live/${tokenId}`, callback);

// 변경 후 (현재 선택된 타입만 구독)
stompClient.subscribe(`/topic/candle/live/${tokenId}/${candleType}`, callback);
// 예: /topic/candle/live/1/MINUTE

---

## 알람 기능 (실시간 헤더 알람)

### 개요

> 로그인한 회원 전용. 매수/매도 주문 체결 또는 배당 지급 시 실시간으로 헤더에 알람 표시.
> STOMP WebSocket + Redis Pub/Sub 방식. 모든 페이지 헤더에 공통 적용.

**알람 발생 조건**
1. 로그인한 회원의 매수 또는 매도 호가가 체결(FILLED/PARTIAL) 되었을 때
2. 배당(allocation)이 지급되는 시점

**DB 부하 여부**
> WebSocket 연결 자체는 persistent connection — 연결 수립 시 1회만 JWT 검증할 뿐, 이후에는 아무 DB 쿼리도 발생하지 않음. 폴링 없음.
> DB 쿼리는 이벤트 발생 시에만 실행 (알람 저장, 읽음 처리). 1만 명이 동시 접속해도 조용하고, 체결·배당 이벤트가 많아지면 그만큼만 INSERT가 늘어나는 구조.
> 결론: **DB 부하 없음** (이벤트 드리븐 단순 INSERT/UPDATE).

---

### [A] 백엔드

#### [A-1] Alarm 엔티티 및 Repository

- [ ] `alarm/entity/AlarmType.java` enum 생성
  ```java
  public enum AlarmType { ORDER_FILLED, DIVIDEND }
  ```
- [ ] `alarm/entity/Alarm.java` 엔티티 생성 (BaseEntity 상속)
  - 필드: `memberId (Long)`, `alarmType (AlarmType)`, `tokenId (Long)`, `message (String)`, `isRead (boolean, default false)`, `createdAt`
  - 인덱스: `(memberId, isRead)` — 미읽음 조회 최적화
- [ ] `alarm/repository/AlarmRepository.java` 생성
  - `findByMemberIdOrderByCreatedAtDesc(Long memberId)` — 전체 목록 (최신순 50건)
  - `findByMemberIdAndIsReadFalse(Long memberId)` — 미읽음 목록
  - `countByMemberIdAndIsReadFalse(Long memberId)` — 미읽음 수
  - `markAllAsRead(Long memberId)` — `@Modifying @Query` bulk update

#### [A-2] AlarmService

- [ ] `alarm/service/AlarmService.java` 인터페이스 생성
- [ ] `alarm/service/AlarmServiceImpl.java` 구현 생성
  - `createAlarm(Long memberId, AlarmType type, Long tokenId, String message)` — DB 저장 + Redis publish
  - `getAlarms(Long memberId)` — 최신순 50건 조회
  - `markAsRead(Long alarmId, Long memberId)` — 단건 읽음 처리 (본인 확인)
  - `markAllAsRead(Long memberId)` — 전체 읽음 처리

#### [A-3] Redis 채널 설정 및 Publisher

- [ ] `RedisPublisher.java`에 `publishAlarm(Long memberId, Object payload)` 메서드 추가
  - 채널: `alarm:{memberId}`
- [ ] `RedisConfig.java`에 `alarm:*` 패턴 구독 리스너 추가
  - `MessageListenerAdapter` + `RedisSubscriber` 연결

#### [A-4] RedisSubscriber — alarm 채널 처리 추가

- [ ] `RedisSubscriber.java`에 `alarm` 타입 케이스 추가
  - 채널명에서 `memberId` 추출 (`alarm:{memberId}` 파싱)
  - `messagingTemplate.convertAndSend("/topic/alarm/{memberId}", payload)` 브로드캐스트

#### [A-5] 체결 이벤트 → 알람 트리거

- [ ] `OrderServiceImpl.processMatchResult()` (또는 match 결과 수신 콜백) 내에서
  - `OrderStatus`가 `FILLED` 또는 `PARTIAL`로 변경될 때
  - 매도자·매수자 각각 `alarmService.createAlarm()` 호출
  - 메시지 예시: `"[토큰명] 매수 주문 12,000원 × 10주 체결 완료"`

> **match 서버 팀원 공유**: match 서버가 체결 후 `trades:{tokenId}` publish 시 `buyerMemberId`, `sellerMemberId`, `tradePrice`, `tradeQuantity` 필드를 포함시켜야 main 서버에서 알람 생성 가능

#### [A-6] 배당 이벤트 → 알람 트리거

- [ ] 배당 지급 서비스(allocation batch 또는 `DividendService`)에서
  - 지급 완료 후 해당 `memberId`에 `alarmService.createAlarm()` 호출
  - 메시지 예시: `"[토큰명] 배당금 3,000원이 지급되었습니다"`

#### [A-7] REST API

- [ ] `alarm/controller/AlarmController.java` 생성 (`@RequestMapping("/api/alarm")`)
  - `GET /api/alarm` — 알람 목록 조회 (JWT memberId 기반)
  - `PATCH /api/alarm/{alarmId}/read` — 단건 읽음 처리
  - `PATCH /api/alarm/read/all` — 전체 읽음 처리
- [ ] `alarm/dto/AlarmResponseDto.java` 생성
  - 필드: `alarmId`, `alarmType`, `tokenId`, `message`, `isRead`, `createdAt`

#### [A-8] Security 설정

- [ ] `SecurityConfig.java` — 아래 경로 인증 필요 추가
  - `/api/alarm/**` — 로그인 필요
  - `/ws/alarm/**` — 로그인 필요 (WebSocket 연결 시 JWT 검증)

#### [A-9] WebSocket 엔드포인트 설정

- [ ] `WebSocketConfig.java`에 `/ws/alarm` 엔드포인트 추가
  - 또는 기존 `/ws/trading`을 공용으로 재활용 (경로 구분은 topic으로)
  - 권장: 기존 `/ws/trading` 그대로 재활용 — 연결 수 줄임, 구독 topic만 추가
- [ ] `AlarmSubscribeHandler.java` 생성
  - `@EventListener(SessionSubscribeEvent)` — `/topic/alarm/{memberId}` 구독 감지
  - JWT에서 추출한 `memberId`와 path variable `{memberId}` 일치 여부 검증 (타인 알람 구독 차단)
  - 구독 시 미읽음 알람 목록 즉시 스냅샷 전송

---

### [B] 프론트엔드 ← 백엔드 완료 후 작업

#### [B-1] `useAlarmSocket` 훅 생성

- [ ] `client/web/src/hooks/useAlarmSocket.js` 생성
  - 로그인 상태일 때만 `/ws/trading` (또는 `/ws/alarm`) 연결
  - `/topic/alarm/{memberId}` 구독
  - 수신 시 알람 목록 state 갱신, 미읽음 count 증가
  - 페이지 이탈/로그아웃 시 구독 해제

#### [B-2] Header 알람 벨 아이콘 + 미읽음 배지

- [ ] `client/web/src/components/layout/Header.jsx` (또는 공통 헤더 컴포넌트)
  - 로그인 상태일 때만 벨 아이콘 렌더링
  - 미읽음 수 > 0 이면 빨간 배지 표시
  - 클릭 시 알람 드롭다운 토글

#### [B-3] 알람 드롭다운 컴포넌트

- [ ] `client/web/src/components/alarm/AlarmDropdown.jsx` 생성
  - 마운트 시 `GET /api/alarm` 호출 → 초기 목록 렌더링
  - 각 항목 클릭 → `PATCH /api/alarm/{alarmId}/read` 호출 → isRead 상태 갱신
  - "전체 읽음" 버튼 → `PATCH /api/alarm/read/all` 호출
  - 읽지 않은 항목 강조 표시 (배경색 or dot)

#### [B-4] 모든 페이지 레이아웃에 적용

- [ ] 메인 페이지(자산 리스트), 상세 페이지, 홈, 내계좌, 관심, 공시·공지 — 공통 레이아웃 또는 각 페이지의 Header에 `useAlarmSocket` 연결 및 `AlarmDropdown` 삽입
  - 공통 `Layout.jsx`가 있으면 거기서 한 번만 처리
  - 없으면 각 페이지 상단에서 `useAlarmSocket` 호출 후 Header에 props 전달
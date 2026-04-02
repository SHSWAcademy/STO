# /trading 페이지 - 차트 / 호가 / 시세 구현 TODO

## 전체 아키텍처 개요

```
[클라이언트]
    │
    ├─ REST  GET /api/token/{tokenId}          → 토큰 기본 정보
    ├─ REST  GET /api/token/{tokenId}/candles  → 캔들 차트 데이터
    │
    └─ WebSocket /ws/trading (STOMP)
           │
           ├─ subscribe /topic/orderbook/{tokenId}  → 호가창 (실시간 미체결 호가)
           └─ subscribe /topic/trades/{tokenId}     → 시세창 (실시간 체결 내역)

[main 서버]
    │
    ├─ 웹소켓 구독 이벤트 감지
    │     └─ match REST GET /internal/orderbook/{tokenId} 호출 → snapshot 즉시 전송
    │
    └─ Redis Subscriber
          ├─ channel: orderbook:{tokenId}  → /topic/orderbook/{tokenId} 브로드캐스트
          └─ channel: trades:{tokenId}     → /topic/trades/{tokenId} 브로드캐스트

[match 서버]
    ├─ 주문 접수 API
    ├─ 호가 변경 시 → Redis publish orderbook:{tokenId}
    └─ 체결 완료 시 → Redis publish trades:{tokenId}
```

---

## 호가창 실시간 방식 결정: 방식 B 채택

### 방식 A — 변경마다 Pub/Sub만 사용
```
주문 접수 → match 처리 → Redis publish → main 수신 → WebSocket 브로드캐스트
```
- **장점**: 구조 단순. 시세창과 완전히 동일한 파이프라인.
- **단점**: WebSocket 최초 연결 시 현재 호가 상태가 없음.
  다음 주문이 들어오기 전까지 클라이언트 화면이 비어있음.

### 방식 B — 초기 Snapshot + 이후 Pub/Sub (채택)
```
[연결 시] main → match REST 호출 → 현재 호가 snapshot 즉시 전송
[변경 시] match → Redis publish → main → WebSocket 브로드캐스트
```
- **장점**: 연결 즉시 현재 호가 상태 표시. 업비트·바이낸스 등 실거래소 표준 패턴.
- **단점**: snapshot REST 경로 + pub/sub 경로 두 가지 관리 필요. 구현 복잡도 약간 증가.
- **채택 이유**: A 방식은 snapshot 없이는 빈 화면 문제가 반드시 발생하므로,
  어차피 snapshot이 필요해지는 A보다 처음부터 B로 설계하는 것이 올바름.

---

## TODO 목록

---

### [0] 사전 정리 작업

- [ ] `TokenDetailDto`에서 1일 최고/최저, 52주 최고/최저 관련 필드 제거
- [ ] `CandleService.getPriceStats()` 및 `PriceStatsDto` 사용 여부 검토 후 제거 또는 유지
  - 현재 `TokenServiceImpl`에서 `CandleService`를 주입받고 있으나 실제로 호출하지 않음 → 정리 필요
- [ ] `TokenController`의 주석 `// 웹소켓 열기 로직 필요` 제거 (웹소켓은 별도 구독 방식으로 처리)

---

### [1] build.gradle 의존성 추가 (main)

- [ ] WebSocket / STOMP 의존성 추가
  ```groovy
  implementation 'org.springframework.boot:spring-boot-starter-websocket'
  ```

---

### [2] WebSocket 설정 (main)

- [ ] `global/config/WebSocketConfig.java` 생성
  - STOMP 엔드포인트: `/ws/trading` (SockJS 폴백 포함)
  - 메시지 브로커: `/topic` (simple broker)
  - 앱 prefix: `/publish`
- [ ] `SecurityConfig`에 WebSocket 경로 permitAll 추가
  - `/ws/trading/**` 인증 없이 접근 허용 (또는 JWT handshake 인터셉터 적용 - 추후 결정)

---

### [3] Redis Pub/Sub 구독 설정 (main)

- [ ] `RedisConfig`에 `RedisMessageListenerContainer` 빈 추가
- [ ] `RedisConfig`에 `MessageListenerAdapter` 빈 추가
- [ ] `trading/websocket/RedisSubscriber.java` 생성
  - `orderbook:{tokenId}` 채널 수신 → `/topic/orderbook/{tokenId}` 브로드캐스트
  - `trades:{tokenId}` 채널 수신 → `/topic/trades/{tokenId}` 브로드캐스트
  - `SimpMessagingTemplate` 사용하여 WebSocket 전송

---

### [4] DTO 설계 (main)

- [ ] `trading/dto/OrderBookDto.java` 생성
  ```
  tokenId, 
  List<OrderBookEntry> bids,   // 매수 호가 (가격 내림차순)
  List<OrderBookEntry> asks,   // 매도 호가 (가격 오름차순)
  timestamp
  
  OrderBookEntry { price, quantity, orderCount }
  ```
- [ ] `trading/dto/TradeDto.java` 생성
  ```
  tokenId, price, quantity, tradeType(BUY/SELL), tradedAt
  ```

---

### [5] 웹소켓 구독 이벤트 감지 → Snapshot 전송 (main, 방식 B 핵심)

- [ ] `trading/websocket/OrderBookSubscribeHandler.java` 생성
  - Spring의 `@EventListener(SessionSubscribeEvent.class)` 사용
  - 구독 destination이 `/topic/orderbook/{tokenId}`인 경우 감지
  - tokenId 파싱 → match 서버에 `GET /internal/orderbook/{tokenId}` REST 호출
  - 결과를 해당 구독자에게 즉시 전송 (`SimpMessagingTemplate.convertAndSendToUser` 또는 전체 브로드캐스트)
- [ ] match 서버 호출용 `MatchClient.java` (RestTemplate 또는 WebClient)
  - `GET {MATCH_SERVER_URL}/internal/orderbook/{tokenId}` → `OrderBookDto` 반환

---

### [6] 캔들 차트 API (main, REST)

- [ ] `CandleController.java` 생성
  - `GET /api/token/{tokenId}/candles?type={minute|hour|day|month|year}&from=...&to=...`
- [ ] `CandleService`에 타입별 캔들 리스트 조회 메서드 추가
  - 각 Repository에서 `tokenId + 시간 범위`로 조회
- [ ] `CandleResponseDto.java` 생성
  ```
  openPrice, highPrice, lowPrice, closePrice, volume, candleTime, tradeCount
  ```
- [ ] 각 CandleRepository에 시간 범위 조회 쿼리 추가 (`findByTokenIdAndCandleTimeBetween`)

---

### [7] match 서버 작업

- [ ] `build.gradle`에 Redis 의존성 추가 (현재 없을 경우)
  ```groovy
  implementation 'org.springframework.boot:spring-boot-starter-data-redis'
  ```
- [ ] 호가 데이터 구조 설계
  - 매수: `TreeMap<Long, Long>` (가격 → 수량, 내림차순)
  - 매도: `TreeMap<Long, Long>` (가격 → 수량, 오름차순)
  - Redis Hash 또는 인메모리로 관리 (결정 필요)
- [ ] 주문 접수 API
  - `POST /api/orders` (main에서 받아서 전달하는 방식이면 internal)
- [ ] 체결 엔진 구현
  - 매수/매도 호가 매칭 로직
- [ ] Redis Publisher 설정
  - 호가 변경 시: `RedisTemplate.convertAndSend("orderbook:{tokenId}", orderBookJson)`
  - 체결 완료 시: `RedisTemplate.convertAndSend("trades:{tokenId}", tradeJson)`
- [ ] 호가 Snapshot API
  - `GET /internal/orderbook/{tokenId}` → 현재 호가 전체 상태 반환 (main의 방식 B snapshot 요청용)

---

### [8] 주문 접수 흐름 (결정 필요)

> 클라이언트 → 주문을 어디서 받을지 아직 미정

- **옵션 1**: `client → main POST /api/orders → main이 match로 REST 전달`
  - 장점: 클라이언트는 main 서버만 바라봄, JWT 인증 처리 자연스러움
  - 단점: main → match 동기 REST 호출 레이턴시
- **옵션 2**: `client → match POST /api/orders 직접 호출`
  - 장점: 레이턴시 감소
  - 단점: 클라이언트가 두 서버를 알아야 하고, 인증 처리를 match에도 구현해야 함

> **현재 추천: 옵션 1** (main이 게이트웨이 역할, 인증은 main에서만)

- [ ] `order/controller/OrderController.java` 생성 (main)
  - `POST /api/orders` - 매수/매도 주문 접수
- [ ] `order/dto/OrderRequestDto.java` 생성
  ```
  tokenId, orderType(BUY/SELL), price, quantity
  ```
- [ ] `OrderService`에서 match 서버로 주문 전달 로직 구현

---

### [9] 프론트엔드 연동 확인 (나중에)

- [ ] `/trading` 페이지에서 `GET /api/token/{tokenId}` 호출 확인
- [ ] WebSocket 연결 (`/ws/trading`) 및 구독 확인
  - `/topic/orderbook/{tokenId}` 구독
  - `/topic/trades/{tokenId}` 구독
- [ ] 캔들 차트 API 연동

---

## 작업 순서 권장

```
[0] 사전 정리
 ↓
[1] build.gradle 의존성
 ↓
[2] WebSocket 설정
 ↓
[4] DTO 설계
 ↓
[3] Redis Pub/Sub 구독 설정  ←── match [7] Redis Publisher와 함께 진행
 ↓
[5] Snapshot 핸들러          ←── match [7] Snapshot API와 함께 진행
 ↓
[6] 캔들 차트 API
 ↓
[8] 주문 접수 흐름
```

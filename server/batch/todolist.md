# Batch 서버 - 캔들 고가/저가 배치 작업 TODO

## 배경 및 흐름
```
[Batch - 1분마다]
  1. DB(TRADES) 조회 → 1분간 체결된 거래에서 토큰별 고가/저가 계산
  2. CANDLEMINUTES 테이블에 저장
  3. Redis publish "candle:{tokenId}:MINUTE" → Main 서버 수신 → WebSocket push
```

---

## Phase 1. 도메인 준비

### 1-1. Entity 작성
- [ ] `candle/entity/CandleMinute.java`
  - CANDLEMINUTES 테이블 매핑
  - 필드: `candleId`, `tokenId`, `openPrice`, `highPrice`, `lowPrice`, `closePrice`, `volume`, `candleTime`, `tradeCount`
- [ ] `trade/entity/Trade.java`
  - TRADES 테이블 매핑 (tradeId, tokenId, tradePrice, tradeQuantity, executedAt만 필요)
- [ ] `token/entity/Token.java`
  - TOKENS 테이블 매핑 (tokenId, tokenStatus만 필요 - TRADING 상태 토큰 목록 조회용)

### 1-2. Repository 작성
- [ ] `trade/repository/TradeRepository.java`
  - 쿼리: 특정 토큰의 특정 시간 범위(1분) 내 체결 목록 조회
  ```java
  // 예시
  List<Trade> findByTokenIdAndExecutedAtBetween(Long tokenId, LocalDateTime from, LocalDateTime to);
  ```
- [ ] `candle/repository/CandleMinuteRepository.java`
  - `save()` 기본 제공
  - 추가 필요 시: 동일 `(tokenId, candleTime)` 존재 여부 확인 쿼리
- [ ] `token/repository/TokenRepository.java`
  - 쿼리: `tokenStatus = TRADING`인 토큰 전체 조회

---

## Phase 2. Redis Publisher 설정

- [ ] `global/config/RedisConfig.java`
  - `RedisTemplate<String, String>` 빈 등록
  - match 서버 RedisConfig와 동일 구조, Subscriber 없이 Publisher만 설정
  ```java
  @Bean
  public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) { ... }
  ```

---

## Phase 3. 서비스 로직 작성

- [ ] `candle/service/CandleMinuteService.java` (interface)
- [ ] `candle/service/CandleMinuteServiceImpl.java`
  - TRADING 상태 토큰 목록 조회
  - 토큰별 루프:
    - 직전 1분(from ~ to) 동안의 TRADES 조회
    - 거래가 없으면 → 해당 분 스킵 (캔들 생성 안 함)
    - 거래가 있으면 → `MAX(tradePrice)` = highPrice, `MIN(tradePrice)` = lowPrice 계산
    - open_price = 해당 분 첫 번째 체결가, close_price = 마지막 체결가
    - volume = SUM(tradeQuantity), tradeCount = 체결 건수
    - CANDLEMINUTES 저장
    - Redis publish `"candle:" + tokenId + ":MINUTE"` 로 저장된 캔들 데이터 JSON publish

> **주의**: Redis publish 채널 포맷은 반드시 `candle:{tokenId}:MINUTE` 형태로 맞출 것
> Main의 `RedisSubscriber.onMessage`에서 `parts[1]`=tokenId, `parts[2]`=타입으로 파싱함

---

## Phase 4. Spring Batch Job 구성

기존 `tokenListing` 배치 패턴(JobConfig → Tasklet → Service) 동일하게 따라갈 것

- [ ] `candle/job/CandleMinuteTasklet.java`
  - `CandleMinuteService.processMinuteCandles()` 호출
  - 로그 출력 (처리된 토큰 수)

- [ ] `candle/job/CandleMinuteJobConfig.java`
  - `candleMinuteJob` Job 빈 등록
  - `candleMinuteStep` Step 빈 등록

- [ ] `candle/job/CandleMinuteScheduler.java`
  - cron: `"0 * * * * *"` (매 분 0초에 실행)
  - `@Qualifier("candleMinuteJob")` 주입
  ```java
  @Scheduled(cron = "0 * * * * *")
  public void runCandleMinuteJob() throws Exception { ... }
  ```

---

## Phase 5. Main 서버 - Redis 구독 패턴 추가 (이미 완료)

- [x] `RedisConfig.java`에 `candle:*`, `pendingOrders:*` 패턴 구독 추가
- [x] `RedisSubscriber.onMessage`에 `candle` 타입 처리 로직 확인

---

## Phase 6. 검증

- [ ] 로컬에서 TRADES 테이블에 더미 데이터 삽입 후 배치 수동 실행 확인
- [ ] CANDLEMINUTES에 high_price, low_price 정상 저장 여부 확인
- [ ] Redis CLI로 publish 메시지 수신 확인
  ```bash
  redis-cli SUBSCRIBE "candle:1:MINUTE"
  ```
- [ ] Main 서버 WebSocket 엔드포인트 `/topic/candle/{tokenId}/MINUTE` 수신 확인

---

## 결정 필요 사항

| 항목 | 선택지 |
|------|--------|
| 1분 내 거래 없을 때 | ~~캔들 row 생성 안 함~~ vs 이전 종가로 빈 캔들 생성 → **거래 없으면 캔들 생성 안 함** (결정 완료) |
| candle_time 기준 | 분 시작(`:00`) vs 분 종료(`:59`) |
| 배치 실패 시 재처리 | Spring Batch 재시도 설정 or 단순 로그 |

# Batch 서버 - 캔들 배치 작업 TODO

## 설계 원칙 (확정)

```
[Batch 역할] — DB 적재 전용
  1분마다:  TRADES 조회 → 토큰별 고가/저가/시가/종가 계산 → CANDLE_MINUTES 저장
  1시간마다: CANDLE_MINUTES 집계 → CANDLE_HOURS 저장
  1일마다:  CANDLE_HOURS 집계 → CANDLE_DAYS 저장
  1달마다:  CANDLE_DAYS 집계 → CANDLE_MONTHS 저장
  1년마다:  CANDLE_MONTHS 집계 → CANDLE_YEARS 저장
  Redis publish 없음 — Batch는 DB 적재만 담당

[과거 봉 제공]
  Main 서버 REST API: GET /api/token/{tokenId}/candle?type=MINUTE 등
  클라이언트가 상세 페이지 진입 시 REST로 과거 봉 조회

[현재 봉 실시간]
  match 서버 체결 완료 → Redis publish("trades:{tokenId}")
  Main 서버 RedisSubscriber 수신 → 현재 봉 고가/저가 갱신 → WebSocket 브로드캐스트
  Batch 개입 없음
```

---

## ✅ 완료된 작업

### Entity / Repository / Job 구성
- [x] `candle/entity/Candle.java` — `@MappedSuperclass` (openPrice, highPrice, lowPrice, closePrice, volume, candleTime, tradeCount, tokenId)
- [x] `candle/entity/CandleMinute.java` — CANDLE_MINUTES 테이블 매핑
- [x] `candle/entity/CandleHour.java` — CANDLE_HOURS 테이블 매핑
- [x] `candle/entity/CandleDay.java` — CANDLE_DAYS 테이블 매핑
- [x] `candle/entity/CandleMonth.java` — CANDLE_MONTHS 테이블 매핑
- [x] `candle/entity/CandleYear.java` — CANDLE_YEARS 테이블 매핑
- [x] `trade/entity/Trade.java` — TRADES 테이블 매핑
- [x] `token/entity/Token.java` — TOKENS 테이블 매핑
- [x] `trade/repository/TradeRepository` — `findByTokenIdAndExecutedAtBetween` 쿼리
- [x] `candle/repository/CandleMinuteRepository` — `findByTokenIdAndCandleTimeBetween` 쿼리
- [x] `candle/repository/CandleHourRepository`, `CandleDayRepository`, `CandleMonthRepository`, `CandleYearRepository`
- [x] `CandleMinuteJobConfig`, `CandleHourJobConfig`, `CandleDayJobConfig`, `CandleMonthJobConfig`, `CandleYearJobConfig`
- [x] `CandleScheduler` — 각 Job 스케줄링 (1분, 1시간, 1일, 1달, 1년)

### Writer Redis publish 제거 (2026-04-07)
- [x] `CandleMinuteWriter` — saveAll만 수행, Redis publish 제거
- [x] `CandleHourWriter` — saveAll만 수행, Redis publish 제거
- [x] `CandleDayWriter` — saveAll만 수행, Redis publish 제거
- [x] `CandleMonthWriter` — saveAll만 수행, Redis publish 제거
- [x] `CandleYearWriter` — saveAll만 수행, Redis publish 제거

---

## ⬜ 남은 작업 (버그 수정)

### [중요] Processor open/close/volume/tradeCount 누락 버그
> 현재 Processor 5개 전부 `highPrice`, `lowPrice`만 빌더에 세팅
> → `openPrice`, `closePrice`, `volume`, `tradeCount`가 NULL로 DB에 저장됨
> → Main REST API 응답에서 해당 필드 NULL로 내려감 → 차트 렌더링 불가

#### CandleMinuteProcessor 수정 (`server.batch.candle.job.candleMinute`)
- [ ] `TradeRepository.findByTokenIdAndExecutedAtBetween` 쿼리에 `ORDER BY executed_at ASC` 추가 확인
- [ ] `openPrice`  = `trades.get(0).getTradePrice()` (첫 번째 체결가)
- [ ] `closePrice` = `trades.get(trades.size() - 1).getTradePrice()` (마지막 체결가)
- [ ] `volume`     = `trades.stream().mapToDouble(Trade::getTradeQuantity).sum()`
- [ ] `tradeCount` = `trades.size()`

#### CandleHourProcessor 수정 (`server.batch.candle.job.candleHour`)
> CandleMinute 리스트를 집계할 때 동일 패턴 적용 (candleTime ASC 정렬 필요)
- [ ] `openPrice`  = 리스트 첫 번째 CandleMinute의 openPrice
- [ ] `closePrice` = 리스트 마지막 CandleMinute의 closePrice
- [ ] `volume`     = CandleMinute volume 합계
- [ ] `tradeCount` = CandleMinute tradeCount 합계

#### CandleDayProcessor, CandleMonthProcessor, CandleYearProcessor 동일하게 수정
- [ ] openPrice  = 집계 대상 리스트 첫 번째의 openPrice
- [ ] closePrice = 집계 대상 리스트 마지막의 closePrice
- [ ] volume     = 합계
- [ ] tradeCount = 합계

---

## 검증

- [ ] TRADES 더미 데이터 삽입 후 1분 배치 수동 실행
- [ ] CANDLE_MINUTES에 open/high/low/close/volume/tradeCount 전부 정상 저장 확인
- [ ] Main `GET /api/token/{tokenId}/candle?type=MINUTE` 호출 시 모든 필드 정상 응답 확인

호가창

- 체결 강도
- 상한가
- 하한가
- 시작가 (오늘 시가)
- 1일 최고
- 1일 최저

---

## 호가창 구현 TODO

### 1. 체결강도
> 현황: WebSocket(`/topic/trades/{tokenId}`) 으로 오는 `TradeEventDto`에 `isBuy` 필드 이미 존재.
> 프론트도 이미 `data.isBuy` 수신 중. 단, 초기 REST 로드 시 방향 정보 없음.

**백엔드**
- [ ] `TradeResponseDto`에 `isBuy` (boolean) 필드 추가
- [ ] `TradeRepository.findTradeList()`에서 `Trade` 엔티티의 `buyOrder` / `sellOrder` 관계를 이용해 방향 판별 후 DTO에 세팅
  - 판별 방법: 해당 체결 기록에서 어느 주문이 나중에 들어와 매칭을 일으켰는지 (taker 기준) → 별도 컬럼 추가 or buyOrder/sellOrder 생성 시간 비교

**프론트**
- [ ] 초기 REST 로드 매핑 시 `isBuy` 필드 반영 (`executions` 초기값 세팅)

---

### 2. 상한가 / 하한가
> 현황: `yesterdayClosePrice`는 이미 `TokenChartDetailResponseDto`에 있고 프론트에서 `basePrice`로 수신 중.
> 계산 공식(`basePrice × 1.3 / × 0.7`)은 프론트에 이미 구현됨.
> 단, 백엔드에서 이 범위를 초과한 주문을 거부하는 검증 로직이 없음.

**백엔드**
- [ ] `OrderServiceImpl` 주문 접수 시 가격 범위 검증 추가
  - 매수/매도 주문 가격이 `yesterdayClosePrice × 0.7` ~ `× 1.3` 범위를 벗어나면 예외 반환

**프론트**
- [ ] "미구현" 뱃지 제거 (백엔드 검증 완료 후)

---

### 3. 시작가 (오늘 시가)
> 현황: 백엔드 `CandleDay` 엔티티에 `openPrice` 존재. `CandleDayRepository.findLatest(tokenId)`로 조회 가능.
> 현재 프론트는 35개 분봉 슬롯의 첫 실제 캔들 시가를 사용 중 → 정확하지 않음.

**백엔드**
- [ ] `TokenChartDetailResponseDto`에 `todayOpenPrice` (Long) 필드 추가
- [ ] `TokenServiceImpl`에서 오늘 날짜 기준 일봉(`CandleDay`)의 `openPrice` 조회 후 DTO에 세팅
  - 오늘 일봉이 없으면 `null` 반환 (거래 없는 날)

**프론트**
- [ ] `tokenInfo.todayOpenPrice`를 시작가로 사용 (null이면 `-` 표시)

---

### 4. 1일 최고 / 1일 최저
> 현황: `CandleDay` 엔티티에 `highPrice`, `lowPrice` 존재.
> 현재 프론트는 35개 분봉 슬롯 내 max/min → 최근 35분 범위만 반영.

**백엔드**
- [ ] `TokenChartDetailResponseDto`에 `todayHighPrice`, `todayLowPrice` (Long) 필드 추가
- [ ] `TokenServiceImpl`에서 오늘 일봉 조회 후 `highPrice`, `lowPrice` 세팅
  - 시작가와 같은 쿼리 결과(`findLatest` 또는 오늘 날짜 필터) 재사용 가능 → 쿼리 1회로 처리

**프론트**
- [ ] `tokenInfo.todayHighPrice` / `todayLowPrice`를 1일최고/최저로 사용 (null이면 `-` 표시)

---

## 수정 범위 요약

| 항목 | 백엔드 수정 | 프론트 수정 | 난이도 |
|------|------------|------------|--------|
| 체결강도 | TradeResponseDto + 방향 판별 로직 | 매핑 1줄 | 중 (방향 판별이 핵심) |
| 상한가/하한가 | 주문 검증 로직 추가 | 뱃지 제거 | 소 |
| 시작가 | DTO 필드 1개 + 서비스 조회 | 값 교체 1줄 | 소 |
| 1일최고/최저 | DTO 필드 2개 + 서비스 조회 | 값 교체 2줄 | 소 (시작가와 같은 쿼리 재사용) |

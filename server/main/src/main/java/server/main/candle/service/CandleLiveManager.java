package server.main.candle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.dto.LiveCandle;
import server.main.candle.entity.*;
import server.main.candle.mapper.CandleMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CandleLiveManager {

    // 5개 주기별 현재 봉 상태 (key가 tokenId)
    private final ConcurrentHashMap<Long, LiveCandle> liveMinute = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandle> liveHour   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandle> liveDay    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandle> liveMonth  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandle> liveYear   = new ConcurrentHashMap<>();

    private final CandleFlushService     candleFlushService;
    private final SimpMessagingTemplate  messagingTemplate;
    private final CandleMapper           candleMapper;

    private final ConcurrentHashMap<Long, Object> tokenLocks = new ConcurrentHashMap<>();

    // RedisSubscriber 에서 체결 수신 시 호출 — 5개 주기 동시 갱신
    public void update(Long tokenId, Double tradePrice, Double tradeQuantity) { // 체결 토큰 id, 체결 가격, 수량
        LocalDateTime now = LocalDateTime.now();

        updateCandle(liveMinute, tokenId, tradePrice, tradeQuantity, now.truncatedTo(ChronoUnit.MINUTES), CandleType.MINUTE);
        updateCandle(liveHour, tokenId, tradePrice, tradeQuantity, now.truncatedTo(ChronoUnit.HOURS), CandleType.HOUR);
        updateCandle(liveDay, tokenId, tradePrice, tradeQuantity, now.truncatedTo(ChronoUnit.DAYS), CandleType.DAY);
        updateCandle(liveMonth, tokenId, tradePrice, tradeQuantity, now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS), CandleType.MONTH);
        updateCandle(liveYear, tokenId, tradePrice, tradeQuantity, now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS), CandleType.YEAR);
    }

    private void updateCandle(ConcurrentHashMap<Long, LiveCandle> map,
                              Long tokenId, Double tradePrice, Double tradeQuantity,
                              LocalDateTime bucketStart, CandleType type) { // bucketStart : 현재 체결이 속하는 봉의 시작 시각
        Object lock = tokenLocks.computeIfAbsent(tokenId, k -> new Object());

        LiveCandle candleToSend;

        synchronized (lock) {
            LiveCandle candle = map.get(tokenId);

            // 캔들 시작 구간이 바뀌었을 경우
            if (candle == null || !candle.getCandleTime().equals(bucketStart)) {
                // 구간 바뀜 → 이전 봉 DB 저장
                if (candle != null) {
                    candleFlushService.saveToDB(candle, tokenId, type);
                }

                // 새 봉 초기화
                candle = LiveCandle.builder()
                        .openPrice(tradePrice)
                        .highPrice(tradePrice)
                        .lowPrice(tradePrice)
                        .closePrice(tradePrice)
                        .volume(tradeQuantity)
                        .tradeCount(1)
                        .candleTime(bucketStart)
                        .build();
            } else {
                // 캔들 시작 구간이 같을 경우 (하나의 캔들 구간에서 여러 체결이 일어날 경우)
                candle.update(tradePrice, tradeQuantity);
            }

            map.put(tokenId, candle);
            candleToSend = candle;
        } // synchronized
        pushToWebSocket(tokenId, candleToSend, type); // lock 밖에서 push
    }

    // 구독 시 현재 봉 스냅샷 반환
    public LiveCandle getSnapshot(Long tokenId, CandleType type) {
        return switch (type) {
            case MINUTE -> liveMinute.get(tokenId);
            case HOUR   -> liveHour.get(tokenId);
            case DAY    -> liveDay.get(tokenId);
            case MONTH  -> liveMonth.get(tokenId);
            case YEAR   -> liveYear.get(tokenId);
        };
    }

    // 체결이 발생할 때마다 현재 봉의 갱신된 상태를 차트 화면에 실시간으로 전송
    private void pushToWebSocket(Long tokenId, LiveCandle candle, CandleType type) {
        CandleResponseDto dto = candleMapper.toLiveDto(candle, type);
        messagingTemplate.convertAndSend("/topic/candle/live/" + tokenId, dto);
    }

    @Scheduled(cron = "0 * * * * *")  // 매 분 0초
    public void flushExpiredCandles() {
        LocalDateTime now = LocalDateTime.now();

        flushMap(liveMinute, CandleType.MINUTE, now.truncatedTo(ChronoUnit.MINUTES));
        flushMap(liveHour,   CandleType.HOUR,   now.truncatedTo(ChronoUnit.HOURS));
        flushMap(liveDay,    CandleType.DAY,    now.truncatedTo(ChronoUnit.DAYS));
        flushMap(liveMonth,  CandleType.MONTH,  now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS));
        flushMap(liveYear,   CandleType.YEAR,   now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS));
    }

    private void flushMap(ConcurrentHashMap<Long, LiveCandle> map, CandleType type, LocalDateTime currentBucket) {
        map.forEach((tokenId, candle) -> {
            Object lock = tokenLocks.computeIfAbsent(tokenId, k -> new Object());
            synchronized (lock) {
                if (!candle.getCandleTime().equals(currentBucket)) {
                    candleFlushService.saveToDB(candle, tokenId, type);
                    map.remove(tokenId);
                }
            }
        });
    }
}

package server.main.candle.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.dto.LiveCandleDto;
import server.main.candle.entity.*;
import server.main.candle.mapper.CandleMapper;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class CandleLiveManager {

    // 5개 주기별 현재 봉 상태 (key tokenId, value LiveCandle)
    private final ConcurrentHashMap<Long, LiveCandleDto> liveMinute = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandleDto> liveHour   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandleDto> liveDay    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandleDto> liveMonth  = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, LiveCandleDto> liveYear   = new ConcurrentHashMap<>();

    private final CandleFlushService     candleFlushService;
    private final SimpMessagingTemplate  messagingTemplate;
    private final CandleMapper           candleMapper;

    // 토큰 ID 별로 락을 따로 구분 (key : tokenId, value : 락)
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

    private void updateCandle(ConcurrentHashMap<Long, LiveCandleDto> map,
                              Long tokenId, Double tradePrice, Double tradeQuantity,
                              LocalDateTime bucketStart, CandleType type) { // bucketStart : 현재 체결이 속하는 봉의 시작 시각
        // 토큰 락 (토큰 id로 생성 또는 조회된다)
        Object lock = tokenLocks.computeIfAbsent(tokenId, k -> new Object());

        // 화면에 보낼 candle
        LiveCandleDto candleToSend;

        synchronized (lock) {
            LiveCandleDto candle = map.get(tokenId); // 해당 캔들 구간

            // 기존 캔들의 시간이 방금 계산한 bucketStart와 다르면(ex : 1분이 지났으면) : 구간이 바뀌었다고 판단
            // 새로운 체결이 들어왔을 때 캔들 기준 시간이 지났으면 이전 캔들을 닫고 saveToDB
            // 문제 : 새로운 체결이 안들어오면 이전 캔들을 saveToDB에 하지 못한다
            // 해결 : 스케줄 어노테이션 -> 1분마다 flush
            if (candle == null || !candle.getCandleTime().equals(bucketStart)) {
                if (candle != null) { // 구간이 바뀌어 새 봉일 경우 이전 봉 DB 저장
                    candleFlushService.saveToDB(candle, tokenId, type);
                    // saveToDB 메서드를 같은 클래스에서 작성하면 @Transactional 동작 x
                    // 별도 빈으로 분리하여 작성 -> 트랜잭션 aop 동작 o
                }

                // 새 봉 생성
                // 새로 들어온 체결가를 시가/고가/저가/종가로 세팅하여 메모리에 둔다
                // 새로 들어온 체결가를 실제 금융권에서 시가, 고가, 저가, 종가로 할당하면서 시작
                candle = LiveCandleDto.builder()
                        .openPrice(tradePrice)
                        .highPrice(tradePrice)
                        .lowPrice(tradePrice)
                        .closePrice(tradePrice)
                        .volume(tradeQuantity)
                        .tradeCount(1)
                        .candleTime(bucketStart)
                        .build();
            } else {
                // 기존 캔들의 시간이 방금 계산한 bucketStart와 같으면
                candle.update(tradePrice, tradeQuantity);
            }

            map.put(tokenId, candle);
            candleToSend = candle;
        } // synchronized 종료 : 락 반납

        pushToWebSocket(tokenId, candleToSend, type); // 캔들 적용하여 화면으로 실시간 push
    }

    // 구독 시 현재 봉 스냅샷 반환
    public LiveCandleDto getSnapshot(Long tokenId, CandleType type) {
        return switch (type) {
            case MINUTE -> liveMinute.get(tokenId);
            case HOUR   -> liveHour.get(tokenId);
            case DAY    -> liveDay.get(tokenId);
            case MONTH  -> liveMonth.get(tokenId);
            case YEAR   -> liveYear.get(tokenId);
        };
    }

    // 체결이 발생할 때마다 현재 봉의 갱신된 상태를 차트 화면에 실시간으로 전송
    private void pushToWebSocket(Long tokenId, LiveCandleDto candle, CandleType type) {
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

    private void flushMap(ConcurrentHashMap<Long, LiveCandleDto> map, CandleType type, LocalDateTime currentBucket) {
        map.forEach((tokenId, candle) -> {
            Object lock = tokenLocks.computeIfAbsent(tokenId, k -> new Object());
            synchronized (lock) {
                if (!candle.getCandleTime().equals(currentBucket)) {
                    candleFlushService.saveToDB(candle, tokenId, type);
                    map.remove(tokenId); // flush 후 map 에서 삭제
                }
            }
        });
    }
}

package server.main.candle.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import server.main.candle.dto.CandleResponseDto;
import server.main.candle.dto.LiveCandleDto;
import server.main.candle.entity.*;
import server.main.candle.mapper.CandleMapper;
import server.main.candle.repository.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
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

    private final CandleMinuteRepository candleMinuteRepository;
    private final CandleHourRepository candleHourRepository;
    private final CandleDayRepository candleDayRepository;
    private final CandleMonthRepository candleMonthRepository;
    private final CandleYearRepository candleYearRepository;

    // 토큰 ID 별로 락을 따로 구분 (key : tokenId, value : 락)
    private final ConcurrentHashMap<Long, Object> tokenLocks = new ConcurrentHashMap<>();

    // RedisSubscriber 에서 체결 수신 시 호출 — 5개 주기 동시 갱신
    public void update(Long tokenId, Long tradePrice, Long tradeQuantity) { // 체결 토큰 id, 체결 가격, 수량
        LocalDateTime now = LocalDateTime.now();

        updateCandle(liveMinute, tokenId, tradePrice, tradeQuantity, now.truncatedTo(ChronoUnit.MINUTES), CandleType.MINUTE);
        updateCandle(liveHour, tokenId, tradePrice, tradeQuantity, now.truncatedTo(ChronoUnit.HOURS), CandleType.HOUR);
        updateCandle(liveDay, tokenId, tradePrice, tradeQuantity, getDayBucket(now), CandleType.DAY);
        updateCandle(liveMonth, tokenId, tradePrice, tradeQuantity, now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS), CandleType.MONTH);
        updateCandle(liveYear, tokenId, tradePrice, tradeQuantity, now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS), CandleType.YEAR);
    }

    private void updateCandle(ConcurrentHashMap<Long, LiveCandleDto> map,
                              Long tokenId, Long tradePrice, Long tradeQuantity,
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

    // 상세 페이지 접근 시 현재 봉 스냅샷 반환
    // 메모리에 없으면 DB 체크포인트로 복원, 체크포인트도 없으면 이전 종가로 근사
    // DB 체크포인트 : 매 분 candle_days에 덮어쓴 데이터
    public LiveCandleDto getSnapshot(Long tokenId, CandleType type) {
        ConcurrentHashMap<Long, LiveCandleDto> map = getMap(type);
        LiveCandleDto live = map.get(tokenId);
        if (live != null) return live;

        LocalDateTime bucketStart = getCurrentBucket(type);

        // DB에서 가장 최근 캔들 조회
        Optional<Candle> latestOpt = findLatestCandle(tokenId, type);
        if (latestOpt.isEmpty()) return null;

        Candle latest = latestOpt.get();

        // 현재 구간 체크포인트가 DB에 있으면 시/고/저/종/거래량 전체 복원 후 메모리에 등록
        if (latest.getCandleTime().equals(bucketStart)) {
            LiveCandleDto restored = LiveCandleDto.builder()
                    .openPrice(latest.getOpenPrice())
                    .highPrice(latest.getHighPrice())
                    .lowPrice(latest.getLowPrice())
                    .closePrice(latest.getClosePrice())
                    .volume(latest.getVolume())
                    .tradeCount(latest.getTradeCount())
                    .candleTime(bucketStart)
                    .build();
            map.putIfAbsent(tokenId, restored); // 동시 체결로 map에 먼저 등록된 봉이 있으면 덮어쓰지 않음 (레이스 컨디션 방지)
            return map.get(tokenId);
        }

        // 현재 구간 체크포인트 없으면 이전 종가로 근사 — map에 저장하지 않음 (시가 오염 방지)
        return LiveCandleDto.builder()
                .openPrice(latest.getClosePrice())
                .highPrice(latest.getClosePrice())
                .lowPrice(latest.getClosePrice())
                .closePrice(latest.getClosePrice())
                .volume(0L)
                .tradeCount(0)
                .candleTime(bucketStart)
                .build();
    }

    // 오늘 일봉 메모리 조회 (DB에 아직 저장 안 된 당일 봉)
    public LiveCandleDto getLiveDay(Long tokenId) {
        return liveDay.get(tokenId);
    }

    // 셧다운 전 메모리에 남아있는 모든 봉을 DB에 저장
    @PreDestroy
    public void flushAllOnShutdown() {
        log.info("[CandleLiveManager] 셧다운 - 메모리 캔들 전체 flush 시작");
        flushAll();
        log.info("[CandleLiveManager] 셧다운 flush 완료");
    }

    private void flushAll() {
        flushMapForce(liveMinute, CandleType.MINUTE);
        flushMapForce(liveHour,   CandleType.HOUR);
        flushMapForce(liveDay,    CandleType.DAY);
        flushMapForce(liveMonth,  CandleType.MONTH);
        flushMapForce(liveYear,   CandleType.YEAR);
    }

    // 구간 만료 여부에 상관없이 현재 메모리의 봉을 모두 DB 저장 (셧다운 전용)
    private void flushMapForce(ConcurrentHashMap<Long, LiveCandleDto> map, CandleType type) {
        map.forEach((tokenId, candle) -> {
            Object lock = tokenLocks.computeIfAbsent(tokenId, k -> new Object());
            synchronized (lock) {
                try {
                    candleFlushService.saveToDB(candle, tokenId, type);
                } catch (Exception e) {
                    log.error("[CandleLiveManager] flush 실패 tokenId={} type={}", tokenId, type, e);
                }
                map.remove(tokenId);
            }
        });
    }

    // type별 live map 반환
    private ConcurrentHashMap<Long, LiveCandleDto> getMap(CandleType type) {
        return switch (type) {
            case MINUTE -> liveMinute;
            case HOUR   -> liveHour;
            case DAY    -> liveDay;
            case MONTH  -> liveMonth;
            case YEAR   -> liveYear;
        };
    }

    // 오전 9시 기준 일봉 버킷 시작 시각 (9시 이후면 오늘 09:00, 이전이면 어제 09:00)
    private LocalDateTime getDayBucket(LocalDateTime now) {
        return now.getHour() >= 9
                ? now.toLocalDate().atTime(9, 0)
                : now.toLocalDate().minusDays(1).atTime(9, 0);
    }

    // 현재 시각 기준 봉 시작 시각 계산
    private LocalDateTime getCurrentBucket(CandleType type) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case MINUTE -> now.truncatedTo(ChronoUnit.MINUTES);
            case HOUR   -> now.truncatedTo(ChronoUnit.HOURS);
            case DAY    -> getDayBucket(now);
            case MONTH  -> now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            case YEAR   -> now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
        };
    }

    // DB에서 해당 토큰의 가장 최근 캔들 전체 조회 — getSnapshot 복원용
    private Optional<Candle> findLatestCandle(Long tokenId, CandleType type) {
        return switch (type) {
            case MINUTE -> candleMinuteRepository.findLatest(tokenId).map(c -> (Candle) c);
            case HOUR   -> candleHourRepository.findLatest(tokenId).map(c -> (Candle) c);
            case DAY    -> candleDayRepository.findLatest(tokenId).map(c -> (Candle) c);
            case MONTH  -> candleMonthRepository.findLatest(tokenId).map(c -> (Candle) c);
            case YEAR   -> candleYearRepository.findLatest(tokenId).map(c -> (Candle) c);
        };
    }

    // 체결이 발생할 때마다 현재 봉의 갱신된 상태를 차트 화면에 실시간으로 전송
    private void pushToWebSocket(Long tokenId, LiveCandleDto candle, CandleType type) {
        CandleResponseDto dto = candleMapper.toLiveDto(candle, type);
        messagingTemplate.convertAndSend("/topic/candle/live/" + tokenId + "/" + type.name(), dto);
    }

    @Scheduled(cron = "0 * * * * *")  // 매 분 0초
    public void flushExpiredCandles() {
        LocalDateTime now = LocalDateTime.now();

        flushMap(liveMinute, CandleType.MINUTE, now.truncatedTo(ChronoUnit.MINUTES));
        flushMap(liveHour,   CandleType.HOUR,   now.truncatedTo(ChronoUnit.HOURS));
        flushMap(liveDay,    CandleType.DAY,    getDayBucket(now));
        flushMap(liveMonth,  CandleType.MONTH,  now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS));
        flushMap(liveYear,   CandleType.YEAR,   now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS));
        // 구간 만료 flush 후 진행 중인 봉 중간 저장 — 크래시 시 유실 범위를 최대 1분으로 제한
        checkpointCurrentCandles();
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

    // 진행 중인 봉(현재 구간)을 map에서 제거하지 않고 DB에 중간 저장
    private void checkpointCurrentCandles() {
        checkpointMap(liveMinute, CandleType.MINUTE);
        checkpointMap(liveHour,   CandleType.HOUR);
        checkpointMap(liveDay,    CandleType.DAY);
        checkpointMap(liveMonth,  CandleType.MONTH);
        checkpointMap(liveYear,   CandleType.YEAR);
    }

    // map에서 제거 없이 upsert만 수행 — 중간 저장용
    private void checkpointMap(ConcurrentHashMap<Long, LiveCandleDto> map, CandleType type) {
        map.forEach((tokenId, candle) -> {
            Object lock = tokenLocks.computeIfAbsent(tokenId, k -> new Object());
            synchronized (lock) {
                try {
                    candleFlushService.saveToDB(candle, tokenId, type);
                } catch (Exception e) {
                    log.error("[CandleLiveManager] checkpoint 실패 tokenId={} type={}", tokenId, type, e);
                }
            }
        });
    }
}

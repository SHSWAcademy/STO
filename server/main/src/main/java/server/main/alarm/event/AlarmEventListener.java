package server.main.alarm.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import server.main.alarm.service.AlarmService;

@Component
@RequiredArgsConstructor
public class AlarmEventListener {

    private final AlarmService alarmService;

    // 트랜잭션 커밋 후 실행 — DB 확정 후 알람 저장 + push
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)  // 내부 트랜잭션 생성
    public void handleAlarmEvent(AlarmEvent event) {
        for (AlarmEvent.AlarmRecord record : event.getRecords()) {
            alarmService.createAlarm( // createAlarm - 내부 트랜잭션 동작
                    record.getMemberId(),
                    record.getAlarmType(),
                    record.getTokenId(),
                    record.getAlarmContent()
            );
        }
    }
}
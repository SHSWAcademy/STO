package server.main.alarm.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import server.main.alarm.entity.AlarmType;

import java.util.List;

@Getter
@AllArgsConstructor
public class AlarmEvent {

    private final List<AlarmRecord> records;

    @Getter
    @AllArgsConstructor
    public static class AlarmRecord {
        private final Long memberId;
        private final AlarmType alarmType;
        private final Long tokenId;
        private final String alarmContent;
    }
}

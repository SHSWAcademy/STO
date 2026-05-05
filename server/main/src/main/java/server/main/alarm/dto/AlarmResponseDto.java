package server.main.alarm.dto;

import lombok.*;
import server.main.alarm.entity.Alarm;
import server.main.alarm.entity.AlarmType;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AlarmResponseDto {
    private Long alarmId;
    private AlarmType alarmType;
    private Long tokenId;
    private String message;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static AlarmResponseDto from(Alarm alarm) {
        return AlarmResponseDto.builder()
                .alarmId(alarm.getAlarmId())
                .alarmType(alarm.getAlarmType())
                .tokenId(alarm.getTokenId())
                .message(alarm.getAlarmContent())
                .isRead(alarm.getIsRead() != null && alarm.getIsRead())
                .createdAt(alarm.getCreatedAt())
                .build();
    }
}

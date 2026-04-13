package server.main.alarm.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import server.main.alarm.dto.AlarmResponseDto;
import server.main.alarm.service.AlarmService;
import server.main.global.security.CustomUserPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/alarm")
@RequiredArgsConstructor
public class AlarmController {

    private final AlarmService alarmService;

    // 알람 목록 조회 (최신순 50건)
    @GetMapping
    public ResponseEntity<List<AlarmResponseDto>> getAlarms() {
        Long memberId = getMemberId();
        return ResponseEntity.ok(alarmService.getAlarms(memberId));
    }

    // 단건 읽음 처리
    @PatchMapping("/{alarmId}/read")
    public ResponseEntity<Void> markAsRead(@PathVariable Long alarmId) {
        Long memberId = getMemberId();
        alarmService.markAsRead(alarmId, memberId);
        return ResponseEntity.noContent().build();
    }

    // 전체 읽음 처리
    @PatchMapping("/read/all")
    public ResponseEntity<Void> markAllAsRead() {
        Long memberId = getMemberId();
        alarmService.markAllAsRead(memberId);
        return ResponseEntity.noContent().build();
    }

    private Long getMemberId() {
        return ((CustomUserPrincipal) SecurityContextHolder.getContext()
                .getAuthentication().getPrincipal()).getId();
    }
}

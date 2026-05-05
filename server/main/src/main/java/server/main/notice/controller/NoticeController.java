package server.main.notice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import server.main.notice.dto.NoticeDetailResponseDTO;
import server.main.notice.dto.NoticeListResponseDTO;
import server.main.notice.dto.NoticeRegisterDTO;
import server.main.notice.service.NoticeService;

@RestController
@RequiredArgsConstructor
@Log4j2
public class NoticeController {
    private final NoticeService noticeService;

    @GetMapping("/api/notice")
    public ResponseEntity<Page<NoticeListResponseDTO>> getPublicNoticeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(noticeService.getPublicNoticeList(page, size));
    }

    @GetMapping("/api/notice/{noticeId}")
    public ResponseEntity<NoticeDetailResponseDTO> getPublicNoticeDetail(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getPublicNoticeDetail(noticeId));
    }

    @GetMapping("/admin/notice")
    public ResponseEntity<Page<NoticeListResponseDTO>> getNoticeList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(noticeService.getNoticeList(page, size));
    }

    @GetMapping("/admin/notice/{noticeId}")
    public ResponseEntity<NoticeDetailResponseDTO> getNoticeDetail(@PathVariable Long noticeId) {
        return ResponseEntity.ok(noticeService.getNoticeDetail(noticeId));
    }

    @PostMapping("/admin/notice")
    public ResponseEntity<Void> registerNotice(@RequestBody NoticeRegisterDTO dto) {
        noticeService.registerNotice(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/admin/notice/{noticeId}")
    public ResponseEntity<Void> updateNotice(@PathVariable Long noticeId, @RequestBody NoticeRegisterDTO dto) {
        noticeService.updateNotice(noticeId, dto);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/notice/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }
}

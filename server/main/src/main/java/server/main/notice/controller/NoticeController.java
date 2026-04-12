package server.main.notice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.main.notice.dto.NoticeDetailResponseDTO;
import server.main.notice.dto.NoticeListResponseDTO;
import server.main.notice.dto.NoticeRegisterDTO;
import server.main.notice.service.NoticeService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
public class NoticeController {
    private final NoticeService noticeService;

    // 공지사항 리스트 조회
    @GetMapping("/admin/notice")
    public ResponseEntity<Page<NoticeListResponseDTO>> getNoticeList(@RequestParam(defaultValue = "0")int page,
                                                                     @RequestParam(defaultValue = "10")int size) {
        Page<NoticeListResponseDTO> list = noticeService.getNoticeList(page, size);
        return ResponseEntity.ok(list);
    }

    // 공지사항 상세 조회
    @GetMapping("/admin/notice/{noticeId}")
    public ResponseEntity<NoticeDetailResponseDTO> getNoticeDetail(@PathVariable Long noticeId) {
        NoticeDetailResponseDTO dto = noticeService.getNoticeDetail(noticeId);
        return ResponseEntity.ok(dto);
    }

    // 공지사항 등록
    @PostMapping("/admin/notice")
    public ResponseEntity<Void> registerNotice(@RequestBody NoticeRegisterDTO dto) {
        noticeService.registerNotice(dto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 공지사항 수정
    @PatchMapping("/admin/notice/{noticeId}")
    public ResponseEntity<Void> updateNotice(@PathVariable Long noticeId, @RequestBody NoticeRegisterDTO dto) {
        noticeService.updateNotice(noticeId, dto);
        return ResponseEntity.ok().build();
    }

    // 공지사항 삭제
    @DeleteMapping("/admin/notice/{noticeId}")
    public ResponseEntity<Void> deleteNotice(@PathVariable Long noticeId) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.noContent().build();
    }

}

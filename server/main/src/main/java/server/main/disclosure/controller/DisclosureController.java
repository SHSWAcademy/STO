package server.main.disclosure.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import server.main.disclosure.dto.DisclosureListResponseDTO;
import server.main.disclosure.dto.DisclosureRegisterDTO;
import server.main.disclosure.dto.DisclosureUpdateDTO;
import server.main.disclosure.service.DisclosureService;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Log4j2
public class DisclosureController {

    private final DisclosureService disclosureService;

    // 공시 내역 조회
    @GetMapping("/admin/disclosure")
    public ResponseEntity<List<DisclosureListResponseDTO>> getDisclosure() {
        List<DisclosureListResponseDTO> list = disclosureService.getDisclosureList();
        return ResponseEntity.ok(list);
    }

    // 공시 등록
    @PostMapping("/admin/disclosure")
    public ResponseEntity<Void> registerDisclosure(@RequestPart DisclosureRegisterDTO dto,
                                                   @RequestPart MultipartFile file) {
        disclosureService.registerDisclosure(dto, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // 공시 수정
    @PatchMapping("/admin/disclosure/{disclosureId}")
    public ResponseEntity<Void> updateDisclosure(@PathVariable Long disclosureId,
                                                 @RequestPart DisclosureUpdateDTO dto,
                                                 @RequestPart(required = false) MultipartFile file) {
        disclosureService.updateDisclosure(disclosureId, dto, file);
        return ResponseEntity.ok().build();
    }

    // 공시 삭제
    @DeleteMapping("/admin/disclosure/{disclosureId}")
    public ResponseEntity<Void> deleteDisclosure(@PathVariable Long disclosureId,
                                                 @RequestParam String storedName) {
        disclosureService.deleteDisclosure(disclosureId, storedName);
        return ResponseEntity.noContent().build();
    }
}

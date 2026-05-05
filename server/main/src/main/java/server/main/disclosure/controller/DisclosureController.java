package server.main.disclosure.controller;

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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import server.main.disclosure.dto.DisclosureListResponseDTO;
import server.main.disclosure.dto.DisclosureRegisterDTO;
import server.main.disclosure.dto.DisclosureUpdateDTO;
import server.main.disclosure.service.DisclosureService;

@RestController
@RequiredArgsConstructor
@Log4j2
public class DisclosureController {

    private final DisclosureService disclosureService;

    @GetMapping("/api/disclosure")
    public ResponseEntity<Page<DisclosureListResponseDTO>> getPublicDisclosure(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(disclosureService.getPublicDisclosureList(page, size));
    }

    @GetMapping("/admin/disclosure")
    public ResponseEntity<Page<DisclosureListResponseDTO>> getDisclosure(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(disclosureService.getDisclosureList(page, size));
    }

    @PostMapping("/admin/disclosure")
    public ResponseEntity<Void> registerDisclosure(@RequestPart DisclosureRegisterDTO dto,
                                                   @RequestPart MultipartFile file) {
        disclosureService.registerDisclosure(dto, file);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/admin/disclosure/{disclosureId}")
    public ResponseEntity<Void> updateDisclosure(@PathVariable Long disclosureId,
                                                 @RequestPart DisclosureUpdateDTO dto,
                                                 @RequestPart(required = false) MultipartFile file) {
        disclosureService.updateDisclosure(disclosureId, dto, file);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/admin/disclosure/{disclosureId}")
    public ResponseEntity<Void> deleteDisclosure(@PathVariable Long disclosureId,
                                                 @RequestParam String storedName) {
        disclosureService.deleteDisclosure(disclosureId, storedName);
        return ResponseEntity.noContent().build();
    }
}

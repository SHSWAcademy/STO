package server.main.global.file;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequiredArgsConstructor
@RequestMapping("/file")
public class FileController {

    private final FileStore fileStore;

    // 이미지 조회 (브라우저에서 바로 표시)
    @GetMapping("/images/{storedName}")
    public ResponseEntity<Resource> getImage(@PathVariable String storedName) throws MalformedURLException {
        Resource resource = getResource(storedName);
        MediaType mediaType = storedName.toLowerCase().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .body(resource);
    }

    // PDF 뷰어 (브라우저에서 인라인으로 표시)
    @GetMapping("/pdf/view/{storedName}")
    public ResponseEntity<Resource> viewPdf(@PathVariable String storedName) throws MalformedURLException {
        Resource resource = getResource(storedName);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + storedName + "\"")
                .body(resource);
    }

    // PDF 다운로드
    @GetMapping("/pdf/download/{storedName}")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String storedName) throws MalformedURLException {
        Resource resource = getResource(storedName);
        String originalName = resource.getFilename();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + originalName + "\"")
                .body(resource);
    }

    // 파일 가져오는 메서드 (공용)
    private Resource getResource(String storedName) {
        // UrlResource 대신 FileSystemResource 사용
        Path filePath = Paths.get(fileStore.getUploadDir()).resolve(storedName).normalize();
        System.out.println("===== 파일 경로: " + filePath); // 확인용 로그
        Resource resource = new FileSystemResource(filePath);
        if (!resource.exists() || !resource.isReadable()) {
            throw new RuntimeException("파일을 찾을 수 없습니다: " + storedName);
        }
        return resource;
    }
}

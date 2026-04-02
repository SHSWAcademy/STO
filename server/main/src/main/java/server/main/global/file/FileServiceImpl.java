package server.main.global.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Log4j2
@RequiredArgsConstructor
public class FileServiceImpl implements FileService{

    private final FileRepository fileRepository;
    private final FileStore fileStore;

    // pdf 파일 등록
    @Override
    public void savePdf(MultipartFile pdfFile, Long disclosureId) {
        // null 검증
        if (pdfFile == null || pdfFile.isEmpty()) return;
        try {
            String storedName = fileStore.saveFile(pdfFile);
            File file = File.builder()
                    .disclosureId(disclosureId)
                    .origin_name(pdfFile.getOriginalFilename())
                    .stored_name(storedName)
                    .path(fileStore.getUploadDir())
                    .size(pdfFile.getSize())
                    .build();

            log.info("파일 저장내역 확인 : {}", file);
            fileRepository.save(file);
        } catch (IOException e) {
            throw new RuntimeException("PDF 파일 저장 실패"+pdfFile.getOriginalFilename(), e);
        }
    }
}

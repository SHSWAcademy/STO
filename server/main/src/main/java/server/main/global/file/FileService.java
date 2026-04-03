package server.main.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    void savePdf(MultipartFile file, Long disclosureId);
}

package server.main.global.file;

import org.springframework.web.multipart.MultipartFile;

public interface FileService {
    void savePdf(MultipartFile file, Long disclosureId);    // pdf파일 저장
    String saveImage(MultipartFile imageFile);      // 자산 이미지 저장
    void deleteFile(String storedName);     // 파일삭제
    String getPdfName(Long disclosureId);    // 원본 파일명 조회
}

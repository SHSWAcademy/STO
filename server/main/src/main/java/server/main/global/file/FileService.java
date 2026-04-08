package server.main.global.file;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    void saveOrUpdatePdf(MultipartFile file, Long disclosureId);    // pdf파일 저장
    String saveImage(MultipartFile imageFile);      // 자산 이미지 저장
    void deleteFile(String storedName);     // 파일삭제
    File getAssetFile(Long disclosureId);    // 원본 파일명 조회
    List<File> getAllocationFile(List<Long> disclosureIds); // 배당 스케줄 파일 조회
}

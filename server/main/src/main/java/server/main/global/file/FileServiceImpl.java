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

    // 이미지 파일 디스크 저장
    @Override
    public String saveImage(MultipartFile imageFile) {
        if (imageFile == null || imageFile.isEmpty()) return null;
        try {
            return fileStore.saveFile(imageFile);
        } catch (IOException e) {
            throw new RuntimeException("이미지 파일 저장 실패", e);
        }
    }

    // 디스크에 저장된 파일 삭제
    @Override
    public void deleteFile(String storedName) {
        if (storedName == null) return;
        new java.io.File(fileStore.getUploadDir(), storedName).delete();
    }

    // 공시ID로 원본 파일명 조회
    @Override
    public String getPdfName(Long disclosureId) {
        File file = fileRepository.findByDisclosureId(disclosureId);
        return file.getStored_name();
    }

    // pdf 파일 등록
    @Override
    public  void savePdf(MultipartFile pdfFile, Long disclosureId) {
        // 저장될 파일명 변수
        String storedName = null;
        // null 검증
        if (pdfFile == null || pdfFile.isEmpty()) return;
        try {
            // 기존 파일이 DB에 있다면 해당 레코드 삭제 / 디스크 파일도 삭제
            File checkFile = fileRepository.findByDisclosureId(disclosureId);
            if (checkFile != null) {
                deleteFile(checkFile.getStored_name());
                fileRepository.delete(checkFile);
            }
            storedName = fileStore.saveFile(pdfFile);
            File file = File.builder()
                    .disclosureId(disclosureId)
                    .origin_name(pdfFile.getOriginalFilename())
                    .stored_name(storedName)
                    .path(fileStore.getUploadDir())
                    .size(pdfFile.getSize())
                    .build();

            log.info("파일 저장내역 확인 : {}", file);
            fileRepository.save(file);

        } catch (Exception e) {
            deleteFile(storedName);
            throw new RuntimeException("PDF 파일 저장 실패: " + pdfFile.getOriginalFilename(), e);
        }
    }
}

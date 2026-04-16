package server.main.global.file;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

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

    // 공시 ID로 건물 소개 파일 조회
    @Override
    public File getAssetFile(Long disclosureId) {
        return fileRepository.findByDisclosureId(disclosureId);
    }

    // 배당 스케줄 증빙 자료 조회
    @Override
    public List<File> getAllocationFile(List<Long> disclosureIds) {
        return fileRepository.findAllByDisclosureIdIn(disclosureIds);
    }

    // pdf 파일 등록
    @Override
    public void saveOrUpdatePdf(MultipartFile pdfFile, Long disclosureId) {
        if (pdfFile == null || pdfFile.isEmpty()) return;
        // 저장될 파일명 변수
        String newStoredName = null;
        // 저장되어있는 변수명
        String oldStoredName = null;
        try {
            // 기존 파일이 DB에 있는지 조회
            File checkFile = fileRepository.findByDisclosureId(disclosureId);
            // 일단 파일먼저 저장 (db는 저장x)
            newStoredName = fileStore.saveFile(pdfFile);

            // 기존파일이 존재하면 수정하고 아니면 저장
            if (checkFile != null) {
                oldStoredName = checkFile.getStoredName();
                checkFile.updateFile(
                        pdfFile.getOriginalFilename(),
                        newStoredName,
                        pdfFile.getSize(),
                        fileStore.getUploadDir()
                );
                log.info("파일 수정내역 확인 : {}", checkFile);
            } else {
                File file = File.builder()
                        .disclosureId(disclosureId)
                        .originName(pdfFile.getOriginalFilename())
                        .storedName(newStoredName)
                        .path(fileStore.getUploadDir())
                        .size(pdfFile.getSize())
                        .build();
                log.info("파일 저장내역 확인 : {}", file);
                fileRepository.save(file);
            }
            // 기존 파일내역 삭제 (신규파일이면 null이라 아무작업안함)
            deleteFile(oldStoredName);
        } catch (IOException e) {
            deleteFile(newStoredName);
            throw new RuntimeException("PDF 파일 저장 실패: " + pdfFile.getOriginalFilename(), e);
        } catch (RuntimeException e) {
            deleteFile(newStoredName);
            throw e;
        }
    }
}

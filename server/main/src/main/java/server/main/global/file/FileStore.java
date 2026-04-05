package server.main.global.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;

@Component
public class FileStore {

    @Value("${file.path}")
    private String filePath;


    // 파일저장
    public String saveFile(MultipartFile file) throws IOException {
        // 원본 파일명
        String originalName = file.getOriginalFilename();
        // 확장자
        String ext = "";

        // 파일명이 null 아니고 . 가 포함되어있는지 확인 (확장자 추출)
        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf("."));
        }

        // 랜덤값 + 확장자 (db업로드 시 중복방지를 위함)
        String dbFileName = UUID.randomUUID().toString()+ext;
        // 저장할경로 + 랜덤으로 설정한 파일이름
        String fullPath = Paths.get(filePath, dbFileName).toString();

        // 업로드할 폴더가 있는지 없다면 생성
        File dir = new File(filePath);
        if(!dir.exists()) dir.mkdirs();

        // 파일저장
        file.transferTo(new File(fullPath));

        // db에 저장할 파일이름 return
        return dbFileName;
    }

    // 파일저장경로 리턴용
    public String getUploadDir() {
        return filePath;
    }
}

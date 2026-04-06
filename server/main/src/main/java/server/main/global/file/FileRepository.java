package server.main.global.file;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FileRepository extends JpaRepository<File, Long> {
    File findByDisclosureId(Long disclosureId);     // 공시ID로 FILE조회
}

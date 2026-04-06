package server.main.global.file;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface FileRepository extends JpaRepository<File, Long> {
    File findByDisclosureId(Long disclosureId);     // 공시ID로 FILE조회
    List<File> findAllByDisclosureIdIn(List<Long> disclosureId); // 배당 배치 리스트 in조회
}

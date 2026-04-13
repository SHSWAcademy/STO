package server.main.notice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import server.main.notice.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findByDeletedAtIsNull(Pageable pageable);
}

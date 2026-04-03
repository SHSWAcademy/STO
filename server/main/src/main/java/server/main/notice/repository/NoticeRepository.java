package server.main.notice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.notice.entity.Notice;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
}

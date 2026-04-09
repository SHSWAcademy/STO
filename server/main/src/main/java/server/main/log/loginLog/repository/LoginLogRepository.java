package server.main.log.loginLog.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.log.loginLog.entity.LoginLog;

public interface LoginLogRepository extends JpaRepository<LoginLog, Long> {
}

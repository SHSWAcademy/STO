package server.main.log.loginLog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import server.main.log.loginLog.entity.LoginLog;
import server.main.log.loginLog.repository.LoginLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String identifier, String ip, String task, String detail, boolean result) {
        loginLogRepository.save(LoginLog.builder()
                .timeStamp(LocalDateTime.now())
                .identifier(identifier)
                .ip(ip)
                .task(task)
                .detail(detail)
                .result(result)
                .build());
    }

    // 로그인 로그 조회 (admin)
    public Page<LoginLog> findLoginLog(Pageable pageable) {
        return loginLogRepository.findAll(pageable);
    }
}

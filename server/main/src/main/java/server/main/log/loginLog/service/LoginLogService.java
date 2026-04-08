package server.main.log.loginLog.service;

import lombok.RequiredArgsConstructor;
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
    public void save(String identifier, String task, String detail, boolean result) {
        loginLogRepository.save(LoginLog.builder()
                .timeStamp(LocalDateTime.now())
                .identifier(identifier)
                .task(task)
                .detail(detail)
                .result(result)
                .build());
    }
}

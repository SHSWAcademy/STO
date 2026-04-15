package server.main.log.tradeLog.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.main.log.tradeLog.entity.TradeLog;
import server.main.log.tradeLog.repository.TradeLogRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class TradeLogService {
    private final TradeLogRepository tradeLogRepository;

    @Transactional
    public void save(String tokenId, String detail, boolean result) {
        tradeLogRepository.save(TradeLog.builder()
                .timeStamp(LocalDateTime.now())
                .identifier(tokenId)
                .task("TRADE")
                .detail(detail)
                .result(result)
                .build());
    }

    // 거래완료 로그 조회 (admin)
    public Page<TradeLog> findTradeLog(Pageable pageable) {
        return tradeLogRepository.findAll(pageable);
    }
}

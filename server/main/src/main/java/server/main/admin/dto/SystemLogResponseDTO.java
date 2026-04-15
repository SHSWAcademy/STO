package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class SystemLogResponseDTO {
    private Long loginLogId;
    private Long orderLogId;
    private Long tradeLogId;
    private String orderType;
    private String ip;
    private LocalDateTime timeStamp;
    private String identifier;
    private String task;
    private String detail;
    private Boolean result;
    private LocalDateTime createdAt;
}

package server.main.admin.dto;

import lombok.*;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AllocationDetailResponseDTO {
    private Long allocationEventId;       // 배당 이벤트ID
    private int settlementYear;           // 배당 지급연도
    private int settlementMonth;          // 배당 지급월
    private LocalDateTime settledAt;       // 정산일
    private Long monthlyDividendIncome;     // 월수익
    private Boolean allocationBatchStatus;  // 지급여부
    private Long disclosureId;              // 공시ID (파일수정용)
    private String originName;   // 원본 파일명 (화면에 표시)
    private String storedName;   // 저장 파일명 (다운로드 시 사용)
}

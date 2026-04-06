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
    private int settlementYear;           // 배당 지급연도
    private int settlementMonth;          // 배당 지급월
    private LocalDateTime settled_at;       // 정산일
    private Long monthlyDividendIncome;     // 월수익
    private String pdfName;                 // 증빙자료명
    private Boolean allocationBatchStatus;  // 지급여부
    private Long fileId;         // 파일ID
    private String originName;   // 원본 파일명 (화면에 표시)
    private String storedName;   // 저장 파일명 (다운로드 시 사용)
}

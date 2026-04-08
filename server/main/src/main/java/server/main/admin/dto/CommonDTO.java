package server.main.admin.dto;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class CommonDTO {
    private Double taxRate; // 세율
    private Double chargeRate; // 수수료
    private int allocateDate; // 배당 지급일
    private int allocateSetDate; // 배당 입력일 (관리자)
}

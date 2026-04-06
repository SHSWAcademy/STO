package server.main.admin.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class AllocationDetailResponseDTO {
    private LocalDateTime allocate_date; // 배당 지급일
    private LocalDateTime allocate_set_date; // 배당 입력일 (관리자)



}

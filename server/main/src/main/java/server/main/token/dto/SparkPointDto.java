package server.main.token.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SparkPointDto {
    // 메인 페이지 각 토큰별로 최근 7일동안 종가 변화율을 보여주는 스파크라인
    private Long value;   // 종가
    private String date;  // 표시용 날짜 문자열
}

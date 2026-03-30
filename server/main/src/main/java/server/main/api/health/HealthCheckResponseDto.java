package server.main.api.health;

import lombok.*;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Data
public class HealthCheckResponseDto {
    private String health;
    private List<String> activeProfiles;
}

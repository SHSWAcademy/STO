package server.main.admin.dto;

import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import server.main.admin.entity.PlatformAccountType;
import server.main.admin.entity.PlatformDirection;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class PlatformBankingListDTO {
    private Long tokenId;   // 토큰ID
    private Long platformBankingAmount; // 입금액
    private LocalDateTime createdAt;

    @Enumerated(value = EnumType.STRING)
    private PlatformAccountType accountType;

    @Enumerated(value = EnumType.STRING)
    private PlatformDirection platformBankingDirection;
}

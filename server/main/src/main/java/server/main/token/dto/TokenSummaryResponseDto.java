package server.main.token.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class TokenSummaryResponseDto {
    private int totalAssets;
    private long totalMarketCap;
    private long todayTradeValue;
    private int upCount;
    private int downCount;
    private List<TopMoverDto> topUp;
    private List<TopMoverDto> topDown;
}

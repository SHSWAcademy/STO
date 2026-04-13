package server.main.admin.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@Builder
@ToString
public class DashBoardTokenList {
    private Long tokenId;               // 토큰ID
    private Long totalSupply;           // 토큰 발행 총 개수
    private Long holdingSupply;         // 플랫폼 보유 토큰
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 심볼 (줄임 표현)
    private Long currentQuantity;       // 현재 회원이 가지고 있는 토큰 보유량
}

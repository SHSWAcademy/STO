package server.main.token.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import server.main.admin.entity.PlatformTokenHolding;
import server.main.asset.entity.Asset;
import server.main.global.util.BaseEntity;

import java.time.LocalDateTime;

@Entity
@Getter
@AllArgsConstructor
@NoArgsConstructor
@SuperBuilder
@ToString
@Table(name = "TOKENS")
public class Token extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long tokenId;               // 토큰ID
    private Long totalSupply;           // 토큰 발행 총 개수
    private Long circulatingSupply;     // 토큰 발행 실제 개수
    private String tokenName;           // 토큰 이름
    private String tokenSymbol;         // 토큰 심볼 (줄임 표현)
    private String contractAddress;     // 온체인 토큰ID
    private Long tokenDecimals;         // ERC20 토큰 메타데이터
    private Long initPrice;             // 토큰 초기 가격
    private Long currentPrice;          // 토큰 현재 가격
    private LocalDateTime issuedAt;     // 실제 거래 가능한 상태로 게시된 시간

    @Enumerated(value = EnumType.STRING)
    private TokenStatus tokenStatus;    // 거래 가능 상태

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id")
    private Asset asset;

    // DB 저장 직전에 자동 실행
    @PrePersist
    public void prePersist() {
        this.issuedAt = LocalDateTime.now();
    }

}

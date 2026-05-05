package server.batch.allocation.processor;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;
import server.batch.allocation.dto.AllocationResult;
import server.batch.allocation.dto.MemberPayoutResult;
import server.batch.allocation.entity.Account;
import server.batch.allocation.entity.AllocationEvent;
import server.batch.allocation.entity.AssetAccount;
import server.batch.allocation.entity.Common;
import server.batch.allocation.entity.TokenHolding;
import server.batch.allocation.repository.AccountRepository;
import server.batch.allocation.repository.AssetAccountRepository;
import server.batch.allocation.repository.CommonRepository;
import server.batch.allocation.repository.PlatformTokenHoldingRepository;
import server.batch.allocation.repository.TokenHoldingRepository;
import server.batch.token.entity.Token;
import server.batch.token.repository.TokenRepository;

import java.util.ArrayList;
import java.util.List;

@Log4j2
@Component
@RequiredArgsConstructor
public class AllocationProcessor implements ItemProcessor<AllocationEvent, AllocationResult> {

    private final TokenRepository tokenRepository;
    private final TokenHoldingRepository tokenHoldingRepository;
    private final PlatformTokenHoldingRepository platformTokenHoldingRepository;
    private final CommonRepository commonsRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final AccountRepository accountRepository;

    @Override
    public AllocationResult process(AllocationEvent event) throws Exception {

        // 자산 계좌 조회
        AssetAccount assetAccount = assetAccountRepository.findByAssetId(event.getAssetId())
                .orElseThrow();

        // asset_id → token_id 조회
        Token tokenId = tokenRepository.findTokenByAssetId(event.getAssetId());
        // 토큰별 유저 토큰 보유량 조회 (0개 이상토큰 들고있을떄)
        List<TokenHolding> tokenHoldings = tokenHoldingRepository.findByTokenIdAndCurrentQuantityGreaterThan(tokenId.getTokenId(),0L);
        // 플랫폼 소유 토큰량 조회
        Long platformTokenHolding = platformTokenHoldingRepository.holdingSupplyByTokenId(tokenId.getTokenId());

        // 전체 수량 계산
        long memberTotalQuantity = 0L;
        // 유저 토큰 수량 누적
        for(TokenHolding tokenHolding : tokenHoldings) {
            memberTotalQuantity += tokenHolding.getCurrentQuantity();
        }
        // 유저 + 플랫폼 보유수량 더하기
        long totalQuantity = memberTotalQuantity + platformTokenHolding;
        log.info("자산ID : {}", event.getAssetId());
        log.info("토큰명 : {}", tokenId.getTokenId());
        log.info("배당 월 수익 : {}", event.getMonthlyDividendIncome());
        log.info("총 토큰 수량 : {}", totalQuantity);
        // 아무도 토크을 안샀다면
        if (totalQuantity == 0) return null;
        // 배당 세율 조회
        Common commons = commonsRepository.findFirstBy();
        // 이전 달 누적 잔여금이 있다면 합산
        long accumulatedRemainder = assetAccount.getAccumulatedRemainder() != null
                ? assetAccount.getAccumulatedRemainder() : 0L;
        long effectiveIncome = event.getMonthlyDividendIncome() + accumulatedRemainder;
        // 배당금 평균 계산
        long allocationAvg = effectiveIncome / totalQuantity;

        // 멤버 배당
        List<MemberPayoutResult> memberPayouts = new ArrayList<>();
        for (TokenHolding tokenHolding : tokenHoldings) {
            // 멤버별 배당금 계산후 list에 add (세전)
            long memberPayout = allocationAvg * tokenHolding.getCurrentQuantity();
            // DB에서 세율 조회후 계산 (소수점 절삭함)
            long tax = (long) (memberPayout * (commons.getTaxRate() / 100));
            long finalPay = memberPayout - tax;
            // 멤버 계좌 조회
            Account account = accountRepository.findByMemberId(tokenHolding.getMemberId())
                    .orElseThrow();
            memberPayouts.add(MemberPayoutResult.builder()
                            .memberId(tokenHolding.getMemberId())
                            .account(account)
                            .payoutAmount(finalPay)
                            .holdingQuantity(tokenHolding.getCurrentQuantity())
                    .build());
        }
        // 플랫폼 배당
        long adminPayout = allocationAvg * platformTokenHolding;
        // 배당 총 지급액
        long totalPaid = allocationAvg * totalQuantity;
        // 실질 수익 - 배당 총 지급액 = 잔여금액
        long remainder = effectiveIncome - totalPaid;

        log.info("누적 잔여금 : {}", accumulatedRemainder);
        log.info("실질 배당 수익 : {}", effectiveIncome);
        log.info("플랫폼 배당금액 : {}", adminPayout);
        log.info("유저 배당금액 : {}", memberPayouts);
        log.info("잔여 금액 : {}", remainder);

        // 계산 완료후 DB에 저장할 데이터 리턴
        return AllocationResult.builder()
                .event(event)
                .tokenId(tokenId.getTokenId())
                .tokenName(tokenId.getTokenName())
                .memberPayouts(memberPayouts)
                .platformAmount(adminPayout)
                .totalDeduction(effectiveIncome)
                .remainder(remainder)
                .assetAccount(assetAccount)
                .build();
    }
}

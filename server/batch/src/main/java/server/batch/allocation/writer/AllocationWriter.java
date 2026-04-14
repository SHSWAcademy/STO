package server.batch.allocation.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import server.batch.allocation.dto.AllocationResult;
import server.batch.allocation.dto.MemberPayoutResult;
import server.batch.allocation.entity.*;
import server.batch.allocation.repository.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log4j2
@Component
@RequiredArgsConstructor
public class AllocationWriter implements ItemWriter<AllocationResult> {

    private final AssetBankingRepository assetBankingRepository;
    private final AssetAccountRepository assetAccountRepository;
    private final AccountRepository accountRepository;
    private final PlatformAccountRepository platformAccountRepository;
    private final PlatformBankingRepository platformBankingRepository;
    private final BankingRepository bankingRepository;
    private final AllocationPayoutRepository allocationPayoutRepository;
    private final AllocationEventRepository allocationEventRepository;
    private final AlarmRepository alarmRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void write(Chunk<? extends AllocationResult> chunk) throws Exception {

        // chunk를 1로 설정해서 0번인덱스 이벤트만 꺼냄
        AllocationResult result = chunk.getItems().getFirst();
        // 자산 계좌에서 배당금 출금처리 (실제 지급된 총액 = totalDeduction - remainder)
        long actualDeduction = result.getTotalDeduction() - result.getRemainder();
        result.getAssetAccount().withdraw(actualDeduction);
        // 누적 잔여금 갱신
        result.getAssetAccount().updateAccumulatedRemainder(result.getRemainder());
        assetAccountRepository.save(result.getAssetAccount());
        // 자산 계좌내역 출금 기록
        AssetBanking assetBanking = AssetBanking.builder()
                .assetAccountId(result.getAssetAccount().getAssetAccountId())
                .assetBankingAmount(actualDeduction)
                .type(AssetBankingType.ALLOCATION)
                .direction(AssetBankingDirection.WITHDRAWAL)
                .build();
        assetBankingRepository.save(assetBanking);
        log.info("자산계좌 출금 확인 : {}", assetBanking);

        // getMemberPayouts에 담긴 멤버 객체 하나씩 꺼냄
        for(MemberPayoutResult member : result.getMemberPayouts()) {
            // 멤버 계좌 입금 처리
            member.getAccount().deposit(member.getPayoutAmount());
            accountRepository.save(member.getAccount());
            // 멤버 계좌내역 기록 (입금)
            Banking banking = Banking.builder()
                    .bankingAmount(member.getPayoutAmount()) // 입금 금액
                    .balanceSnapshot(member.getAccount().getAvailableBalance()) // 계좌 최종잔고
                    .txStatus(TxStatus.SUCCESS)
                    .txType(TxType.DEPOSIT)
                    .accountId(member.getAccount().getAccountId())
                    .build();
            bankingRepository.save(banking);

            // 배당 지급 이력 저장
            AllocationPayout payout = AllocationPayout.builder()
                    .memberId(member.getMemberId())
                    .allocationEventId(result.getEvent().getAllocationEventId())
                    .tokenId(result.getTokenId())
                    .memberIncome(member.getPayoutAmount())
                    .holdingQuantity(member.getHoldingQuantity())
                    .status(PayoutStatus.SUCCESS)
                    .build();
            allocationPayoutRepository.save(payout);
            log.info("배당 지급 기록 : {}", payout);

            // 배당 알람 DB 저장 + Redis publish — 실패해도 배당 입금에 영향 없도록 격리
            try {
                String alarmContent = String.format("[ %s ] 배당금 %,d원이 지급되었습니다.",
                        result.getTokenName(), member.getPayoutAmount());
                Alarm alarm = Alarm.builder()
                        .memberId(member.getMemberId())
                        .tokenId(result.getTokenId())
                        .alarmType(AlarmType.DIVIDEND)
                        .alarmContent(alarmContent)
                        .build();
                Alarm savedAlarm = alarmRepository.save(alarm);

                Map<String, Object> payload = new HashMap<>();
                payload.put("alarmId",   savedAlarm.getAlarmId());
                payload.put("alarmType", "DIVIDEND");
                payload.put("tokenId",   result.getTokenId());
                payload.put("message",   alarmContent);
                payload.put("isRead",    false);
                if (savedAlarm.getCreatedAt() != null) {
                    payload.put("createdAt", savedAlarm.getCreatedAt().toString());
                }
                redisTemplate.convertAndSend("alarm:" + member.getMemberId(),
                        objectMapper.writeValueAsString(payload));
                log.info("[Alarm] 배당 알람 발행 - memberId: {}", member.getMemberId());
            } catch (Exception e) {
                log.error("[Alarm] 배당 알람 처리 실패 (배당 입금은 정상 완료) - memberId: {}", member.getMemberId(), e);
            }
        }
        // 플랫폼 계좌 업데이트
        PlatformAccount platformAccount = platformAccountRepository.findFirstBy();
        platformAccount.deposit(result.getPlatformAmount());
        platformAccountRepository.save(platformAccount);

        // 플랫폼 계좌 입금내역 기록
        PlatformBanking platformBanking = PlatformBanking.builder()
                .platformBankingAmount(result.getPlatformAmount())
                .accountType(PlatformAccountType.DIVIDEND)
                .platformBankingDirection(PlatformDirection.DEPOSIT)
                .tokenId(result.getTokenId())
                .build();
        platformBankingRepository.save(platformBanking);
        log.info("플랫폼 계좌 입금내역 : {}", platformBanking);

        // 배당 이벤트 완료, 배당지급일 등록 처리 (detached 엔티티라 명시적 save 필요)
        result.getEvent().complete();
        allocationEventRepository.save(result.getEvent());
    }
}

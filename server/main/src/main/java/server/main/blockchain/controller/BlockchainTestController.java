//package server.main.blockchain.controller;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Profile;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import server.main.blockchain.service.TokenIssueService;
//import server.main.global.error.BusinessException;
//import server.main.global.error.ErrorCode;
//import server.main.member.entity.Member;
//import server.main.member.repository.MemberRepository;
//import server.main.token.entity.Token;
//import server.main.token.repository.TokenRepository;
//import server.main.trade.entity.SettlementStatus;
//import server.main.trade.entity.Trade;
//
//import java.time.LocalDateTime;
//
//@RestController
//@RequestMapping("test/blockchain")
//@RequiredArgsConstructor
//@Profile("!prod")
//public class BlockchainTestController {
//
//    private final TokenIssueService tokenIssueService;
//    private final TokenRepository tokenRepository;
//    private final MemberRepository memberRepository;
//
//    @PostMapping("recordTrade")
//    public ResponseEntity<String> testRecordTrade(
//            @RequestParam Long tokenId,
//            @RequestParam Long sellerId,
//            @RequestParam Long buyerId,
//            @RequestParam Long quantity,
//            @RequestParam Long price
//    ) {
//        Token token = tokenRepository.findById(tokenId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_NOT_FOUND));
//
//        Member seller = memberRepository.findById(sellerId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
//
//        Member buyer = memberRepository.findById(buyerId)
//                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
//
//        Trade fakeTrade = Trade.builder()
//                .tradePrice(price)
//                .tradeQuantity(quantity)
//                .totalTradePrice(price * quantity)
//                .feeAmount(0L)
//                .settlementStatus(SettlementStatus.ON_CHAIN_PENDING)
//                .executedAt(LocalDateTime.now())
//                .seller(seller)
//                .buyer(buyer)
//                .token(token)
//                .build();
//
//        tokenIssueService.recordTrade(fakeTrade);
//        return ResponseEntity.ok("recordTrade 성공");
//
//    }
//}

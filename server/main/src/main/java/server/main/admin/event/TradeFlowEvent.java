package server.main.admin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TradeFlowEvent {
    private String stage;       // PENDING / OUTBOX_PROCESSING / SUCCESS / FAILED
    private Long tradeId;
    private Long tokenId;
    private String tokenSymbol;
    private Long amount;
    private Long qty;
    private String buyerName;
    private String sellerName;
}

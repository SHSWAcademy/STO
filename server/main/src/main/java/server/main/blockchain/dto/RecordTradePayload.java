package server.main.blockchain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordTradePayload {
    private String contractAddress;
    private String sellerAddress;
    private String buyerAddress;
    private Long quantity;
    private Long price;
}

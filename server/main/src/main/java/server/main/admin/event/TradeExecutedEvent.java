package server.main.admin.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import server.main.admin.dto.DashBoardTradeListDTO;

@Getter
@AllArgsConstructor
public class TradeExecutedEvent {
    private DashBoardTradeListDTO dashBoardTradeListDTO;
}

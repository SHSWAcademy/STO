package server.main.trade.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RestController;
import server.main.trade.service.TradeService;

@RestController
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

}

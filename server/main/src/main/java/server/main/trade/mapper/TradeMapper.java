package server.main.trade.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import server.main.trade.dto.TradeResponseDto;
import server.main.trade.entity.Trade;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TradeMapper {
    public TradeResponseDto toDto(Trade trade);
}

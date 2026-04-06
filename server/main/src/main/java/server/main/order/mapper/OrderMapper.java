package server.main.order.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import server.main.order.dto.PendingOrderResponseDto;
import server.main.order.entity.Order;

import java.util.List;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {
    PendingOrderResponseDto toPendingDto(Order order);
    List<PendingOrderResponseDto> toPendingDtoList(List<Order> orders);
}

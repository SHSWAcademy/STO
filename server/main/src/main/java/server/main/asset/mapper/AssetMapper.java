package server.main.asset.mapper;

import org.mapstruct.Mapper;
import server.main.asset.dto.AssetMainResponseDto;
import server.main.asset.entity.Asset;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    AssetMainResponseDto toMainDto(Asset asset);
}

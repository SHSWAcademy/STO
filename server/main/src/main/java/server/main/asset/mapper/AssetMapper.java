package server.main.asset.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import server.main.asset.dto.AssetDetailDto;
import server.main.asset.entity.Asset;

@Mapper(componentModel = "spring")
public interface AssetMapper {
    @Mapping(source = "id", target = "assetId")
    AssetDetailDto toDto(Asset asset);

    Asset toEntity(AssetDetailDto dto);
}

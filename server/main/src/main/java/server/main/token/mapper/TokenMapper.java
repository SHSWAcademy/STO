package server.main.token.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import server.main.token.dto.TokenChartDetailResponseDto;
import server.main.token.entity.Token;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TokenMapper {

    // asset 필드에서 가져오는 데이터
    @Mapping(source = "asset.assetName",    target = "assetName")
    @Mapping(source = "asset.imgUrl",       target = "imgUrl")
    TokenChartDetailResponseDto toDtoDetail(Token token);
    Token toEntityDetail(TokenChartDetailResponseDto tokenDetailDto);
}

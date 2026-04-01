package server.main.token.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import server.main.token.dto.TokenDetailDto;
import server.main.token.entity.Token;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TokenMapper {

    // asset 필드에서 가져오는 데이터
    @Mapping(source = "asset.assetName",    target = "assetName")
    @Mapping(source = "asset.imgUrl",       target = "imgUrl")
    TokenDetailDto toDtoDetail(Token token);
    Token toEntityDetail(TokenDetailDto tokenDetailDto);
}

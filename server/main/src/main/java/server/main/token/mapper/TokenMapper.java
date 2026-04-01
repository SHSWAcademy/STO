package server.main.token.mapper;

import ch.qos.logback.core.subst.Token;
import org.mapstruct.Mapper;
import server.main.token.dto.TokenDTO;

@Mapper(componentModel = "spring")
public interface TokenMapper {
    TokenDTO toDto(Token token);
    Token toEntity(TokenDTO tokenDTO);
}

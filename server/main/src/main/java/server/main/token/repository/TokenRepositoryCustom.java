package server.main.token.repository;

import server.main.token.dto.SelectType;
import server.main.token.entity.Token;

import java.util.List;

public interface TokenRepositoryCustom {
    List<Token> findAllBySelectType(int page, SelectType selectType);
}

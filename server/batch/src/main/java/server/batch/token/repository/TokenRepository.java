package server.batch.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.batch.token.entity.Token;
import server.batch.token.entity.TokenStatus;

import java.util.List;

public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findAllByTokenStatus(TokenStatus tokenStatus);
}

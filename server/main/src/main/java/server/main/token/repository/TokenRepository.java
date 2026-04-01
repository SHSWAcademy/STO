package server.main.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import server.main.token.entity.Token;

public interface TokenRepository extends JpaRepository<Token, Long> {
}

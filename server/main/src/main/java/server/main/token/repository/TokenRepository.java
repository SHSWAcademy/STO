package server.main.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.main.token.entity.Token;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {

    // fetch join 으로 asset 테이블 조인해서 데이터를 가져온다
    @Query("SELECT t FROM Token t JOIN FETCH t.asset WHERE t.tokenId =:tokenId")
    Optional<Token> findByIdWithAsset(@Param("tokenId") Long tokenId);
}

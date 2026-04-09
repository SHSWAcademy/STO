package server.batch.token.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import server.batch.token.entity.Token;
import server.batch.token.entity.TokenStatus;

import java.util.List;

public interface TokenRepository extends JpaRepository<Token, Long> {
    List<Token> findAllByTokenStatus(TokenStatus tokenStatus);

    @Query("SELECT c FROM Token c WHERE c.assetId = :assetId")
    Token findTokenByAssetId(@Param("assetId") Long assetId);   // 자산ID로 토큰조회 (배당 배치용)
}

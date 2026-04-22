package server.main.token.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import server.main.token.entity.Token;
import server.main.token.entity.TokenStatus;

public interface TokenRepository extends JpaRepository<Token, Long> , TokenRepositoryCustom {

    // fetch join 으로 asset 테이블 조인해서 데이터를 가져온다
    @Query("SELECT t FROM Token t JOIN FETCH t.asset WHERE t.tokenId =:tokenId")
    Optional<Token> findByIdWithAsset(@Param("tokenId") Long tokenId);

    // 자산 ID 로 토큰 / 자산테이블 리스트 조회용
    @Query("SELECT t FROM Token t JOIN FETCH t.asset ORDER BY CASE WHEN t.tokenStatus = 'TRADING' THEN 0 ELSE 1 END ASC, t.createdAt DESC")
    List<Token> findAllTokensWithAsset();

    // 자산 ID 로 토큰 / 자산테이블 배당 리스트 조회용
    @Query("SELECT t FROM Token t JOIN FETCH t.asset a WHERE t.tokenStatus = 'TRADING' AND a.isAllocated = true ")
    List<Token> findAllTokensWithAssetAllocationList();

    // 토큰테이블, 멤버 토큰 보유 테이블 조회 (admin)
    @Query(""" 
            SELECT t, COALESCE(SUM(h.currentQuantity + h.lockedQuantity), 0)  
            FROM Token t 
            LEFT JOIN MemberTokenHolding h ON h.token = t 
            WHERE t.tokenStatus = 'TRADING' 
            GROUP BY t 
           """)
    List<Object[]> findTradingTokensWithTotalHolding();

    @Query("SELECT t FROM Token t JOIN FETCH t.asset a WHERE a.assetId = :assetId")
    Optional<Token> findByAssetIdWithAsset(@Param("assetId") Long assetId);

    @Query("SELECT t FROM Token t JOIN FETCH t.asset WHERE t.tokenStatus = 'TRADING'")
    List<Token> findAllTradingTokensWithAsset();

    Long tokenId(Long tokenId);

    List<Token> findAllByTokenStatus(TokenStatus tokenStatus);
}

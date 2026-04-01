package server.main.global.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    private SecretKey secretKey;

    @PostConstruct
    public void init() {
        byte[] keyBytes = Base64.getDecoder().decode(secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
    }

    public String createMemberToken(Long memberId, String email) {
        return createToken(memberId, email, "MEMBER", "ROLE_USER");
    }

    public String createAdminToken(Long adminId, String adminLoginId) {
        return createToken(adminId, adminLoginId, "ADMIN", "ROLE_ADMIN");
    }

    private String createToken(Long id, String loginId, String userType, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + accessTokenExpiration);

        return Jwts.builder()
                .subject(String.valueOf(id))
                .claim("loginId", loginId)
                .claim("userType", userType)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT: {}", e.getMessage());
        }
        return false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public CustomUserPrincipal getPrincipal(String token) {
        Claims claims = getClaims(token);
        return new CustomUserPrincipal(
                Long.valueOf(claims.getSubject()),
                claims.get("loginId", String.class),
                claims.get("userType", String.class),
                claims.get("role", String.class)
        );
    }
}

package server.batch.token.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenListingServiceImpl implements TokenListingService {

    // NOTE:
    // The batch module does not currently share the Token JPA entity
    // with the main module, so the first version uses JdbcTemplate.
    // This can be refactored to Repository/JPA later if the modules
    // adopt a shared domain strategy.
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public int openIssuedTokens() {
        String sql = """
                UPDATE tokens
                SET token_status = 'TRADING'
                WHERE token_status = 'ISSUED'
                """;

        int updatedCount = jdbcTemplate.update(sql);
        log.info("ISSUED -> TRADING updated count: {}", updatedCount);
        return updatedCount;
    }
}

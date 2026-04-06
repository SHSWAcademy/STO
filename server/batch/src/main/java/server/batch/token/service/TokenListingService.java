package server.batch.token.service;

// 토큰 상장 전환 작업을 수행하는 역할
// Tasklet이 SQL까지 직접 알지 않게 하려고
// 나중에 구현체를 바꾸기 쉽게 하려고
// 배치 실행 흐름과 비즈니스 로직을 분리하려고
public interface TokenListingService {
    int openIssuedTokens();
}

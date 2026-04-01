package server.main.token.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import server.main.token.repository.TokenRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class TokenServiceImpl implements TokenService{

    private final TokenRepository tokenRepository;
}

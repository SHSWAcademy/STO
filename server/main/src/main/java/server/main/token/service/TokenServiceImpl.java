package server.main.token.service;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import server.main.token.repository.TokenRepository;

@Service
@RequiredArgsConstructor
@Log4j2
public class TokenServiceImpl implements TokenService{

    private final TokenRepository tokenRepository;
}

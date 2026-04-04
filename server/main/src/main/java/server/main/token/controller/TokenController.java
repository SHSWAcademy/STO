package server.main.token.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import server.main.token.dto.TokenDetailDto;
import server.main.token.service.TokenService;

@RestController
@RequiredArgsConstructor
@Slf4j
public class TokenController {

    private final TokenService tokenService;

    // 토큰 (자산) 상세 조회
    @GetMapping("/{tokenId}")
    public ResponseEntity<TokenDetailDto> tokenDetails(@PathVariable Long tokenId) {

        TokenDetailDto dto = tokenService.getTokenDetail(tokenId);
        log.info(String.valueOf(dto));
        return ResponseEntity.ok(dto);
    }
}

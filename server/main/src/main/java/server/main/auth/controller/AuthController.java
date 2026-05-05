package server.main.auth.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import server.main.auth.dto.AdminLoginRequest;
import server.main.auth.dto.LoginResponse;
import server.main.auth.dto.MemberLoginRequest;
import server.main.auth.dto.MemberSignupRequest;
import server.main.auth.dto.MemberSignupResponse;
import server.main.auth.service.AuthService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    
    @PostMapping("/member/signup")
    public ResponseEntity<MemberSignupResponse> memberSignup(@Valid @RequestBody MemberSignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/member/login")
    public ResponseEntity<LoginResponse> memberLogin(@Valid @RequestBody MemberLoginRequest request,
                                                        HttpServletRequest httpServletRequest) {
        return ResponseEntity.ok(authService.memberLogin(request, httpServletRequest));
    }

    @PostMapping("/admin/login")
    public ResponseEntity<LoginResponse> adminLogin(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(authService.adminLogin(request));
    }
}

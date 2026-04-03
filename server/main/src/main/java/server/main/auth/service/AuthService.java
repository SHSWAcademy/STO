package server.main.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import server.main.admin.entity.Admin;
import server.main.admin.repository.AdminRepository;
import server.main.auth.dto.AdminLoginRequest;
import server.main.auth.dto.LoginResponse;
import server.main.auth.dto.MemberLoginRequest;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.security.JwtTokenProvider;
import server.main.member.entity.Member;
import server.main.member.repository.MemberRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    // timing attack 완화용 dummy hash - BCrypt(cost=10)로 생성된 유효한 해시
    private static final String DUMMY_HASH = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";

    private final MemberRepository memberRepository;
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse memberLogin(MemberLoginRequest request) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(request.getEmail()).orElse(null);

        if (member == null) {
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH); // timing 완화
            log.warn("[AUTH] 회원 로그인 실패 - 회원 조회 실패 (미가입 또는 비활성): email={}", maskEmail(request.getEmail()));
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getMemberPassword())) {
            log.warn("[AUTH] 회원 로그인 실패 - 비밀번호 불일치: memberId={}", member.getMemberId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtTokenProvider.createMemberToken(member.getMemberId(), member.getEmail());
        return new LoginResponse(token, "MEMBER");
    }

    public LoginResponse adminLogin(AdminLoginRequest request) {
        Admin admin = adminRepository.findByAdminLoginId(request.getAdminLoginId())
                .orElse(null);

        if (admin == null) {
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH); // timing 완화
            log.warn("[AUTH] 관리자 로그인 실패 - 관리자 조회 실패: loginId={}", maskId(request.getAdminLoginId()));
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.getPassword(), admin.getAdminLoginPassword())) {
            log.warn("[AUTH] 관리자 로그인 실패 - 비밀번호 불일치: adminId={}", admin.getAdminId());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        String token = jwtTokenProvider.createAdminToken(admin.getAdminId(), admin.getAdminLoginId());
        return new LoginResponse(token, "ADMIN");
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***";
        int atIndex = email.indexOf('@');
        String local = email.substring(0, atIndex);
        String domain = email.substring(atIndex);
        if (local.length() <= 2) return "**" + domain;
        return local.substring(0, 2) + "***" + domain;
    }

    private String maskId(String id) {
        if (id == null || id.length() <= 2) return "***";
        return id.substring(0, 2) + "***";
    }
}

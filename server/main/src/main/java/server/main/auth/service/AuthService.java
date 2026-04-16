package server.main.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import server.main.admin.entity.Admin;
import server.main.admin.event.AdminDashboardEvent;
import server.main.admin.repository.AdminRepository;
import server.main.auth.dto.AdminLoginRequest;
import server.main.auth.dto.LoginResponse;
import server.main.auth.dto.MemberLoginRequest;
import server.main.auth.dto.MemberSignupRequest;
import server.main.auth.dto.MemberSignupResponse;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.security.JwtTokenProvider;
import server.main.log.loginLog.service.LoginLogService;
import server.main.myAccount.entity.Account;
import server.main.member.entity.Member;
import server.main.member.entity.Wallet;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.MemberRepository;
import server.main.member.service.CustodialWalletService;

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
    private final AccountRepository accountRepository;
    private final LoginLogService loginLogService;
    private final CustodialWalletService custodialWalletService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public MemberSignupResponse signup(MemberSignupRequest request) {
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        String encodedPassword = passwordEncoder.encode(request.getPassword());
        Member member = Member.create(request.getEmail(), encodedPassword, request.getName());
        memberRepository.save(member);

        String accountNumber = generateUniqueAccountNumber();
        String encodedAccountPassword = passwordEncoder.encode(request.getAccountPassword());
        Account account = Account.create(member, accountNumber, encodedAccountPassword);
        accountRepository.save(account);

        Wallet wallet = custodialWalletService.createMemberWallet(member);

        // 어드민 대시보드 (회원가입 시 이벤트 소켓)
        eventPublisher.publishEvent(new AdminDashboardEvent());
        return new MemberSignupResponse(member.getMemberId(), member.getEmail(), member.getMemberName(), wallet.getWalletAddress());
    }

    public LoginResponse memberLogin(MemberLoginRequest request, HttpServletRequest httpServletRequest) {
        Member member = memberRepository.findByEmailAndIsActiveTrue(request.getEmail()).orElse(null);
        String ClientIp = getClientIp(httpServletRequest);

        if (member == null) {
            passwordEncoder.matches(request.getPassword(), DUMMY_HASH); // timing 완화
            log.warn("[AUTH] 회원 로그인 실패 - 회원 조회 실패 (미가입 또는 비활성): email={}", maskEmail(request.getEmail()));
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.getPassword(), member.getMemberPassword())) {
            log.warn("[AUTH] 회원 로그인 실패 - 비밀번호 불일치: memberId={}", member.getMemberId());
            loginLogService.save(request.getEmail(),ClientIp, "MEMBER_LOGIN", "로그인 실패", false);
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 회원 로그 저장
        loginLogService.save(request.getEmail(),ClientIp, "MEMBER_LOGIN", "로그인 성공", true);

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



    private String generateUniqueAccountNumber() {
        for (int i =0; i < 10; i++) {
            String accountNumber = String.format("%010d", ThreadLocalRandom.current().nextLong(0, 10_000_000_000L));
            if (!accountRepository.existsByAccountNumber(accountNumber)) {
                return accountNumber;
            }
        }
        throw new IllegalStateException("계좌번호 생성에 실패했습니다.");
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

    // 클라이언트 IP 기록용 (admin)
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}

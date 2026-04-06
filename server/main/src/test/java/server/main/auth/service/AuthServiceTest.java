package server.main.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import server.main.admin.repository.AdminRepository;
import server.main.auth.dto.LoginResponse;
import server.main.auth.dto.MemberLoginRequest;
import server.main.auth.dto.MemberSignupRequest;
import server.main.auth.dto.MemberSignupResponse;
import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.global.security.JwtTokenProvider;
import server.main.member.entity.Member;
import server.main.member.repository.AccountRepository;
import server.main.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private AccountRepository accountRepository;

    // ── 회원가입 테스트 ────────────────────────────────────────────

    @Test
    @DisplayName("회원가입 성공 시 회원과 계좌가 함께 저장된다")
    void signup_success() {
        // given
        MemberSignupRequest request = createSignupRequest("user@test.com", "Password1!", "홍길동", "1234");

        given(memberRepository.existsByEmail("user@test.com")).willReturn(false);
        given(passwordEncoder.encode(anyString())).willReturn("encoded");
        given(accountRepository.existsByAccountNumber(anyString())).willReturn(false);

        // when
        MemberSignupResponse response = authService.signup(request);

        // then
        verify(memberRepository).save(any(Member.class));
        verify(accountRepository).save(any());
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getName()).isEqualTo("홍길동");
    }

    @Test
    @DisplayName("이미 존재하는 이메일로 회원가입 시 실패한다")
    void signup_fail_emailDuplicate() {
        // given
        MemberSignupRequest request = createSignupRequest("dup@test.com", "Password1!", "홍길동", "1234");

        given(memberRepository.existsByEmail("dup@test.com")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.EMAIL_ALREADY_EXISTS);

        verify(memberRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
    }

    @Test
    @DisplayName("정상 로그인 시 JWT를 반환한다")
    void memberLogin_success(){
        // given
        MemberLoginRequest request = createLoginRequest("user@test.com", "password123");
        Member member = createMember(1L, "user@test.com", "encodedPassword");

        given(memberRepository.findByEmailAndIsActiveTrue("user@test.com"))
            .willReturn(Optional.of(member));
        given(passwordEncoder.matches("password123", "encodedPassword"))
            .willReturn(true);
        given(jwtTokenProvider.createMemberToken(1L, "user@test.com"))
            .willReturn("jwt.token.here");
        // when
        LoginResponse response = authService.memberLogin(request);
        // then
        assertThat(response.getAccessToken()).isEqualTo("jwt.token.here");
        assertThat(response.getUserType()).isEqualTo("MEMBER");
    }

    @Test                                                                                                            
    @DisplayName("존재하지 않는 이메일로 로그인 시 실패한다")                                                      
    void memberLogin_fail_memberNotFound() {
        // given
        MemberLoginRequest request = createLoginRequest("unknown@test.com", "password");

        given(memberRepository.findByEmailAndIsActiveTrue("unknown@test.com"))
            .willReturn(Optional.empty());
        given(passwordEncoder.matches(anyString(), anyString()))
            .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.memberLogin(request))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("비밀번호 불일치 시 실패한다")
    void memberLogin_fail_wrongPassword() {
        // given
        MemberLoginRequest request = createLoginRequest("user@test.com", "wrongPassword");
        Member member = createMember(1L, "user@test.com", "encodedPassword");

        given(memberRepository.findByEmailAndIsActiveTrue("user@test.com"))
                .willReturn(Optional.of(member));
        given(passwordEncoder.matches("wrongPassword", "encodedPassword"))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.memberLogin(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("비활성 회원으로 로그인 시 실패한다")
    void memberLogin_fail_inactiveMember() {
        // given - isActive=false 회원은 findByEmailAndIsActiveTrue 에서 조회되지 않음
        MemberLoginRequest request = createLoginRequest("inactive@test.com", "password123");

        given(memberRepository.findByEmailAndIsActiveTrue("inactive@test.com"))
                .willReturn(Optional.empty());
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> authService.memberLogin(request))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.LOGIN_FAILED);
    }

    @Test
    @DisplayName("회원이 없을 때 timing attack 완화를 위해 dummy 비밀번호 비교를 실행한다")
    void memberLogin_fail_timingAttack() {
        // given
        MemberLoginRequest request = createLoginRequest("unknown@test.com", "anyPassword");

        given(memberRepository.findByEmailAndIsActiveTrue("unknown@test.com"))
                .willReturn(Optional.empty());
        given(passwordEncoder.matches(anyString(), anyString()))
                .willReturn(false);

        // when
        assertThatThrownBy(() -> authService.memberLogin(request))
                .isInstanceOf(BusinessException.class);

        // then - dummy hash 로 matches 가 반드시 한 번 호출됨
        verify(passwordEncoder).matches(eq("anyPassword"), anyString());
    }

    // ── 헬퍼 메서드 ────────────────────────────────────────────

    private MemberSignupRequest createSignupRequest(String email, String password, String name, String accountPassword) {
        try {
            MemberSignupRequest request = new MemberSignupRequest();
            Field emailField = MemberSignupRequest.class.getDeclaredField("email");
            Field passwordField = MemberSignupRequest.class.getDeclaredField("password");
            Field nameField = MemberSignupRequest.class.getDeclaredField("name");
            Field accountPasswordField = MemberSignupRequest.class.getDeclaredField("accountPassword");
            emailField.setAccessible(true);
            passwordField.setAccessible(true);
            nameField.setAccessible(true);
            accountPasswordField.setAccessible(true);
            emailField.set(request, email);
            passwordField.set(request, password);
            nameField.set(request, name);
            accountPasswordField.set(request, accountPassword);
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private MemberLoginRequest createLoginRequest(String email, String password) {
        try {
            MemberLoginRequest request = new MemberLoginRequest();
            Field emailField = MemberLoginRequest.class.getDeclaredField("email");
            Field passwordField = MemberLoginRequest.class.getDeclaredField("password");
            emailField.setAccessible(true);
            passwordField.setAccessible(true);
            emailField.set(request, email);
            passwordField.set(request, password);
            return request;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private Member createMember(Long id, String email, String encodedPassword) {
        try {
            Member member = new Member();
            Field idField = Member.class.getDeclaredField("memberId");
            Field emailField = Member.class.getDeclaredField("email");
            Field passwordField = Member.class.getDeclaredField("memberPassword");
            idField.setAccessible(true);
            emailField.setAccessible(true);
            passwordField.setAccessible(true);
            idField.set(member, id);
            emailField.set(member, email);
            passwordField.set(member, encodedPassword);
            return member;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

package server.main.member.controller;

import static org.mockito.BDDMockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.lang.reflect.Field;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import server.main.global.config.CorsConfig;
import server.main.global.config.SecurityConfig;
import server.main.global.security.CustomUserPrincipal;
import server.main.global.security.JwtAccessDeniedHandler;
import server.main.global.security.JwtAuthenticationEntryPoint;
import server.main.global.security.JwtTokenProvider;
import server.main.member.entity.Member;
import server.main.member.service.MemberService;

@WebMvcTest(MemberController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtAuthenticationEntryPoint.class, JwtAccessDeniedHandler.class})
class MemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MemberService memberService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("토큰 없이 요청하면 401을 반환한다")
    void getMyInfo_noToken_unauthorized() throws Exception {
        mockMvc.perform(get("/api/members/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효하지 않은 JWT로 요청하면 401을 반환한다")
    void getMyInfo_invalidToken_unauthorized() throws Exception {
        given(jwtTokenProvider.validateToken("invalid.jwt.token")).willReturn(false);

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer invalid.jwt.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 JWT로 요청하면 200과 회원 정보를 반환한다")
    void getMyInfo_validToken_success() throws Exception {
        String validToken = "valid.jwt.token";
        CustomUserPrincipal principal = new CustomUserPrincipal(1L, "user@test.com", "MEMBER", "ROLE_USER");
        Member member = createMember(1L, "user@test.com", "홍길동");

        given(jwtTokenProvider.validateToken(validToken)).willReturn(true);
        given(jwtTokenProvider.getPrincipal(validToken)).willReturn(principal);
        given(memberService.getMyInfo(1L)).willReturn(member);

        mockMvc.perform(get("/api/members/me")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberId").value(1L))
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.name").value("홍길동"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    private Member createMember(Long id, String email, String name) {
        try {
            Member member = new Member();
            Field idField = Member.class.getDeclaredField("memberId");
            Field emailField = Member.class.getDeclaredField("email");
            Field nameField = Member.class.getDeclaredField("memberName");
            idField.setAccessible(true);
            emailField.setAccessible(true);
            nameField.setAccessible(true);
            idField.set(member, id);
            emailField.set(member, email);
            nameField.set(member, name);
            return member;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

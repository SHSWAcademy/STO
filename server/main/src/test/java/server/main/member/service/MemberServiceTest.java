package server.main.member.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.BDDMockito.*;

import java.lang.reflect.Field;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import server.main.global.error.BusinessException;
import server.main.global.error.ErrorCode;
import server.main.member.dto.MemberMeResponse;
import server.main.member.entity.Member;
import server.main.member.repository.MemberRepository;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @InjectMocks
    private MemberServiceImpl memberService;

    @Mock
    private MemberRepository memberRepository;

    @Test
    @DisplayName("정상적인 memberId로 조회 시 회원 정보를 반환한다")
    void getMyInfo_success() {
        // given
        Member member = createMember(1L, "user@test.com", "홍길동");
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        MemberMeResponse response = memberService.getMyInfo(1L, "ROLE_USER");

        // then
        assertThat(response.getMemberId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getName()).isEqualTo("홍길동");
        assertThat(response.getRole()).isEqualTo("ROLE_USER");
    }

    @Test
    @DisplayName("존재하지 않는 memberId로 조회 시 MEMBER_NOT_FOUND 예외를 던진다")
    void getMyInfo_fail_memberNotFound() {
        // given
        given(memberRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMyInfo(99L, "ROLE_USER"))
                .isInstanceOf(BusinessException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.MEMBER_NOT_FOUND);
    }

    // ── 헬퍼 메서드 ────────────────────────────────────────────

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
